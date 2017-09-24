// -*- java -*-

package eem.frame.motion;

import eem.frame.core.*;
import eem.frame.motion.*;
import eem.frame.dangermap.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import robocode.util.*;

import java.util.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.awt.Color;



public class exactPathDangerMotion extends basicMotion {
	protected fighterBot myBot;
	protected double superDanger = Double.POSITIVE_INFINITY;
	dangerPath path = new dangerPath();
	long stopTime = (long) ( Math.ceil(robocode.Rules.MAX_VELOCITY/robocode.Rules.DECELERATION) );
	long antiRammerPathTics = 32;
	// To  not end up moving into the wall without chance to stop 
	// We need at least
	// stopTime tick, but than our planning horizon is to shallow
	// Which is bad against rammers, so I chose  the largest of it
	long minimalPathLength = (long) ( Math.max( 32, 2*stopTime ) ); 
	// tune below to avoid skipped turns
	long maximalPathLength = 50;
	// TODO looks like I made some speed up, so it might be better to
	// do small initial trial number and push up trials to improve
	long nTrials = 20;
	long nTrialsToImprove = 2;
	
	long wrongPathPredictionCount = 0;
	
	public void initTic() {
		// here I check exact path simulator
		if ( path.size() >= 1) {
			long curTime = myBot.getTime();
			while ( path.getFirst().getBotStatPoint().getTime() < curTime ) {
				// this point from the past, may be we had skipped turns
				// or its new turn but we are holding old path
				path.removeFirst();
				if ( path.size() == 0 ) {
					break;
				}
			}
		}
		if ( (path.size() >= 1) && !needToRecalculate ) {
			// NOTE: this is for algorithm mistakes notifications
			if ( myBot.getPosition().distance( path.getFirst().getPosition() ) > 1 ) {
				//logger.warning("--- Check path simulator! ---");
				//logger.warning("tic time: " + myBot.getTime() );
				//logger.warning("path size " + path.size() );
				//logger.warning("current  path point = " + myBot.getStatClosestToTime( myBot.getTime() ).format() );
				//logger.warning("expected path point = " + path.getFirst().toString() );
				wrongPathPredictionCount++;
				needToRecalculate = true;
			}
			// end of algorithm check
			if ( path.size() >= 1 ) {
				path.removeFirst();
			}
		} else {
			needToRecalculate = true;
		}
		if (needToRecalculate) {
			choseNewPath( Math.max( predictionEndTime - myBot.getTime(), maximalPathLength ), nTrials );
			needToRecalculate = false;
		} else {
			// routine update with smaller number of trials
			choseNewPath( Math.max( predictionEndTime - myBot.getTime(), maximalPathLength ), nTrialsToImprove );
		}

	}

	public exactPathDangerMotion() {
	}

	public exactPathDangerMotion(fighterBot bot) {
		myBot = bot;
		initBattle( myBot );
		destPoint = new dangerPoint( new Point2D.Double(0,0), superDanger);
	}

	public void manage() {
		//choseNewPath();
		if ( path.size() <= minimalPathLength ) {
			needToRecalculate = true;
		}
		moveToPoint( destPoint.getPosition() );
		// end of exact check
	}

	public void choseNewPath() {
		choseNewPath( maximalPathLength, nTrials );
	}

	public void choseNewPath( long pathLength, long nTrials ) {
		profiler.start( "choseNewPath" );

		dangerPath pathTrial;
		pathLength = (long) math.putWithinRange( pathLength, minimalPathLength, maximalPathLength );
		Point2D.Double myPos = (Point2D.Double) myBot.getPosition().clone();
		// first we try to reuse old destination point
		Point2D.Double pp = (Point2D.Double) destPoint.getPosition().clone();
		path = new dangerPath( pathSimulator.getPathTo( pp, myBot.getStatClosestToTime( myBot.getTime() ), pathLength ) );
		path.calculateDanger( myBot, superDanger );
		//path.setDanger(superDanger); // crazy dangerous for initial sorting

		double a = 0; // angle to new target candidate
		double headOnAngle = 0;
		Point2D.Double pivotPnt = new Point2D.Double(physics.BattleField.getX()/2, physics.BattleField.getY()/2);
		headOnAngle = Math.toRadians( math.game_angles2cortesian(math.angle2pt( myPos, pivotPnt ) ) );
		for ( long i = 0; i < nTrials; i++ ) {
			pp = new Point2D.Double(0,0);
			// FIXME make final decision
			// for now I disable the orthogonal to the enemy motion
			// IWillFireNowBullet has smaller APS with this enabled
			// see difference between v1.6 and v1.8
			// so we are back to search for good destination
			// within a circle surrounding the  bot
			if ( ( myBot.getGameInfo().fightType().equals("1on1") || myBot.getGameInfo().fightType().equals("melee1on1") ) && (myBot.getGunManager().getTarget() != null ) ) {
			//if ( false ) {
				// 1on1 game type and I have target
				// let's try to move mostly orthogonal to the path to target
				
				// angle orthogonal to the line to enemy
				fighterBot tmpEnemyBot = myBot.getGunManager().getTarget();
				if ( tmpEnemyBot != null ) {
					pivotPnt = tmpEnemyBot.getPosition();
					headOnAngle = Math.toRadians( math.game_angles2cortesian(math.angle2pt( myPos, pivotPnt ) ) );
					a = headOnAngle + Math.PI/2.;
					// random spread to it
					double angleSpread = Math.PI/4.;
					a += angleSpread*(Math.random() - 0.5);
					if ( Math.random() > 0.5 ) {
						// shift angle 180 degree to flip direction
						a += Math.PI;
					}
				} else {
					a = 2*Math.PI* Math.random();
				}
			} else {
				// searching at any angle around us
				a= 2*Math.PI * Math.random();
			}
			double da = Math.PI/180.*math.signNoZero( math.shortest_arc( Math.toDegrees( headOnAngle - a ) ) );
			double R = pathLength*robocode.Rules.MAX_VELOCITY * Math.random();
			do {
				pp.x = myPos.x + R*Math.cos( a ); 
				pp.y = myPos.y + R*Math.sin( a ); 
				a += da; // we will move final point into battlefield
			} while ( !physics.botReacheableBattleField.contains( pp ) );
			pathTrial = new dangerPath( pathSimulator.getPathTo( pp, myBot.getStatClosestToTime( myBot.getTime() ), pathLength ) );
			pathTrial.calculateDanger( myBot, path.getDanger() );
			if ( path.getDanger() > pathTrial.getDanger() ) {
				//logger.dbg("Choosing new path with danger = " + pathTrial.getDanger()); 
				path = pathTrial;
				destPoint = new dangerPoint( pp, 0 );
			}
		}
		profiler.stop( "choseNewPath" );
	}

	public void makeMove() {
		// for basic motion we do nothing
	}

	public void reportStats() {
		logger.routine("wrong path prediction count: " +  wrongPathPredictionCount + " <------------------------------ improve me!");
	}

	public void drawFullDangerMap(Graphics2D g) {
		// here I draw full danger map picture at current time
		dangerMap _dangerMapFull = new dangerMap( myBot );
		// now I populate the map points grid
		int Npx = 20;
		int Npy = 20;
		for( int i=0; i < Npx; i++ ) {
			for( int k=0; k < Npy; k++ ) {
				Point2D.Double p = new Point2D.Double( 0,0 );
				p.x = physics.BattleField.x / (Npx+1) * (i+1) ;
				p.y = physics.BattleField.y / (Npy+1) * (k+1) ;
				_dangerMapFull.add( p );
			}
		}
		_dangerMapFull.calculateDanger( myBot.getTime() );
		_dangerMapFull.onPaint(g);
	}

	public void onPaint(Graphics2D g) {
		// mark destination point
		g.setColor(new Color(0x00, 0xff, 0x00, 0x80));
		graphics.drawCircle(g, destPoint.getPosition(), 10);

		path.onPaint(g);
		//drawFullDangerMap(g);
	}

}
