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



public class waveSurfingMotion extends exactPathDangerMotion {
	dangerPath clockWisePath = new dangerPath();
	dangerPath counterClockWisePath = new dangerPath();
	int CLOCKWISE = 0;
	int COUNTERCLOCKWISE = 1;
	boolean clockwise = true;

	public waveSurfingMotion(fighterBot bot) {
		super(bot);
	}

	public void choseNewPath( long pathLength, long nTrials ) {
		profiler.start( "waveSurfingMotion.choseNewPath" );

		dangerPath pathTrial;
		pathLength = (long) math.putWithinRange( pathLength, minimalPathLength, maximalPathLength );
		//pathLength = 30; // maximum time between waves
		Point2D.Double myPos = (Point2D.Double) myBot.getPosition().clone();
		// first we try to reuse old destination point
		Point2D.Double pp = (Point2D.Double) destPoint.getPosition().clone();
		path = new dangerPath( pathSimulator.getPathTo( pp, myBot.getStatClosestToTime( myBot.getTime() ), pathLength ) );
		path.calculateDanger( myBot, superDanger );
		path.shortenToWaveHit();
		//path.setDanger(superDanger); // crazy dangerous for initial sorting

		double a = 0; // angle to new target candidate
		double headOnAngle = 0;
		Point2D.Double pivotPnt = new Point2D.Double(physics.BattleField.getX()/2, physics.BattleField.getY()/2);
		headOnAngle = Math.toRadians( math.game_angles2cortesian(math.angle2pt( myPos, pivotPnt ) ) );
		for ( long i = 0; i <= 1; i++ ) {
			// two trials clock wise and counter clockwise
			pp = new Point2D.Double(0,0);
			// FIXME make final decision
			// for now I disable the orthogonal to the enemy motion
			// IWillFireNowBullet has smaller APS with this enabled
			// see difference between v1.6 and v1.8
			// so we are back to search for good destination
			// within a circle surrounding the  bot
			if (  (myBot.getGunManager().getTarget() != null ) ) {
				// I have target
				// let's try to move mostly orthogonal to the path to target
				
				// angle orthogonal to the line to enemy
				fighterBot tmpEnemyBot = myBot.getGunManager().getTarget();
				if ( tmpEnemyBot != null ) {
					pivotPnt = tmpEnemyBot.getPosition();
					headOnAngle = Math.toRadians( math.game_angles2cortesian(math.angle2pt( myPos, pivotPnt ) ) );
					// i=0 clockwise
					// i=1 counter clockwise
					a =  Math.PI/2.; // will be 90 degree to enemy
					a += Math.PI/8;  // and slightly away
					clockwise = false;
				        if ( i == COUNTERCLOCKWISE) {
						a = -a;
						clockwise = true;
					}
					a += headOnAngle;
				} else {
					a = 2*Math.PI* Math.random();
				}
			} else {
				// searching at any angle around us
				a= 2*Math.PI * Math.random();
			}
			double da = Math.PI/180.*math.signNoZero( math.shortest_arc( Math.toDegrees( headOnAngle - a ) ) );
			clockwise = true;
			if ( da >= 0 ) {
				clockwise = false;
			}
			double R = pathLength*robocode.Rules.MAX_VELOCITY;
			do {
				pp.x = myPos.x + R*Math.cos( a ); 
				pp.y = myPos.y + R*Math.sin( a ); 
				a += da; // we will move final point into battlefield
			} while ( !physics.botReacheableBattleField.contains( pp ) );
			pathTrial = new dangerPath( pathSimulator.getPathTo( pp, myBot.getStatClosestToTime( myBot.getTime() ), pathLength ) );
			pathTrial.calculateDanger( myBot, path.getDanger() ); // also find hit by wave point
			pathTrial.shortenToWaveHit();
			if ( path.getDanger() > pathTrial.getDanger() ) {
				//logger.dbg("Choosing new path with danger = " + pathTrial.getDanger() + " and length " + pathTrial.size()); 
				path = pathTrial;
				destPoint = new dangerPoint( path.getLast().getPosition(), path.getLast().getDanger() );
			}
		}
		profiler.stop( "waveSurfingMotion.choseNewPath" );
	}

}
