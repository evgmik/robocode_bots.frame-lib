// -*- java -*-

package eem.frame.wave;

import eem.frame.core.*;
import eem.frame.bot.*;
import eem.frame.gun.*;
import eem.frame.misc.*;

import robocode.Bullet;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.*;

public class wave {
	public InfoBot firedBot = null;
	protected Point2D.Double firedPosition;
	protected long   firedTime;
	protected double bulletSpeed;
	protected double bulletEnergy;
	protected long timeUncertaintyLower = 0; // how sure we are about firing time
	protected Color waveColor = new Color(0xff, 0x00, 0x00, 0x80);
	protected Color waveUncertaintyColorLower = new Color(0x00, 0x00, 0xff, 0x80);
	protected int count=0; // wave count for a particular bot
	public boolean realWave = false; // if wave is real or virtual

	public LinkedList<safetyCorridor> safetyCorridors = new LinkedList<safetyCorridor>();
	// small safety corridors could be miscalculated, also they consume time to process
	// but if they are small they are unlikely to change the wave danger anyway
	private double safetyCorridorSizeThreshold = 1e-10; // degrees

	public wave(InfoBot firedBot, long firedTime, double bulletEnergy) {
		this.firedBot = firedBot;
		this.firedTime = firedTime;
		this.bulletEnergy = bulletEnergy;
		this.bulletSpeed = physics.bulletSpeed( bulletEnergy );
		Point2D.Double fP = firedBot.getPositionAtTime( firedTime );
		if ( fP != null ) {
			this.firedPosition = (Point2D.Double) fP.clone();
			this.timeUncertaintyLower = 0;
		} else {
			// we do not know position of the bot at firing time
			// but we do know that exactly the time upper bound
			// which is firedTime since it is coming from onScannedRobot
			// so we will use last known (i.e. current) position
			//FIXME uncertainty logic
			this.firedPosition = (Point2D.Double) firedBot.getLast().getPosition().clone();
			this.timeUncertaintyLower = firedTime - firedBot.getPrev().getTime();
			logger.noise("timeUncertaintyLower = " + timeUncertaintyLower );
		}
	}

	public InfoBot getFiredBot() {
		return firedBot;
	}

	public Point2D.Double getFiredPosition() {
		return (Point2D.Double) firedPosition.clone();
	}

	public long getFiredTime() {
		return firedTime;
	}

	public double getBulletSpeed() {
		return bulletSpeed;
	}

	public void setCount(int c) {
		count = c;
	}

	public int getCount() {
		return count;
	}

	public double getBulletEnergy() {
		return bulletEnergy;
	}

	public double getDistanceTraveledAtTime(long time) {
		double timeInFlight = time - firedTime;
		double distTraveled = timeInFlight * bulletSpeed;
		return distTraveled;
	}

	public boolean isBehindBot(InfoBot bot, long timeNow) {
		double distTraveled = getDistanceTraveledAtTime( timeNow );
		Point2D.Double botPos = bot.getPositionClosestToTime( timeNow );
		double distToBot = botPos.distance( firedPosition ); 
		if ( distTraveled > distToBot + physics.robotHalfDiagonal )
			return true;
		else
			return false;
	}

	public boolean isPassingOverBotPosition(Point2D.Double botPos, long timeNow) {
		if ( botPos == null ) {
			// we do not know the position of the bot at this time
			// so we report false to avoid wrong hit counts
			return false;
		}
		double distTraveled = getDistanceTraveledAtTime( timeNow );
		double distToBot = botPos.distance( firedPosition );
		if ( Math.abs(distTraveled - distToBot) <= physics.robotHalfDiagonal )
			return true;
		else
			return false;
	}
	public boolean isPassingOverBot(InfoBot bot, long timeNow) {
		if ( firedBot.getName().equals( bot.getName() ) ) {
			// this is the wave of the fired bot
			// no need to check for passing
			return false;
		}
		Point2D.Double botPos = bot.getPosition( timeNow );
		return isPassingOverBotPosition( botPos, timeNow );
	}

	public double getTimeToReach( Point2D.Double p ) {
		double distToTravel = p.distance( firedPosition );
		return distToTravel/bulletSpeed;
	}

	public double getFiringAngleOffset( InfoBot bot, long time ) {
		Point2D.Double tgtPosNow =        bot.getPositionClosestToTime( time );
		double hitAngle    = math.angle2pt( firedPosition, tgtPosNow);
		double headOnAngle = getHeadOnAngle( bot );
		return math.shortest_arc(hitAngle - headOnAngle);
	}

	public double getHeadOnAngle( InfoBot bot ) {
		Point2D.Double tgtPosAtFiredTime = bot.getPositionClosestToTime( firedTime );
		double headOnAngle = math.angle2pt( firedPosition, tgtPosAtFiredTime);
		return headOnAngle; // degrees
	}

	public double getFiringGuessFactorRange( InfoBot bot, long time ) {
		double dist = bot.getStatClosestToTime( time ).getPosition().distance( firedPosition );
		double gfRange = 2*Math.atan(physics.robotHalfDiagonal/dist)/Math.toRadians( physics.calculateMEA( bulletSpeed ) );
		return gfRange;
	}

	public double getFiringGuessFactor( InfoBot bot, long time ) {
		//double lateralSpeed = bot.getStatClosestToTime(firedTime).getLateralSpeed( firedPosition );
		//logger.dbg("lat speed = " + lateralSpeed );
		//return math.signNoZero(lateralSpeed)*getFiringAngleOffset(bot, time)/physics.calculateMEA( bulletSpeed );
		return getFiringAngleOffset(bot, time)/physics.calculateMEA( bulletSpeed );
	}

	public double getFiringGuessFactor( InfoBot bot, double absFiringAngle ) {
		//double lateralSpeed = bot.getStatClosestToTime(firedTime).getLateralSpeed( firedPosition );
		double firingAngleOffset = math.shortest_arc( absFiringAngle - getHeadOnAngle( bot ) );

		//return math.signNoZero(lateralSpeed)*firingAngleOffset/physics.calculateMEA( bulletSpeed );
		return firingAngleOffset/physics.calculateMEA( bulletSpeed );
	}

	public double getDistanceAtLastAimTime( InfoBot bot ) {
		Point2D.Double tPos = bot.getStatClosestToTime(firedTime - 1).getPosition();
		return firedPosition.distance( tPos );
	}


	public boolean equals( wave w ) {
		boolean ret = true;
		if ( !this.getFiredBot().getName().equals( w.getFiredBot().getName() ) )
			return false;
		if ( this.getFiredTime() != w.getFiredTime() )
			return false;
		if ( this.getBulletEnergy() != w.getBulletEnergy() )
			return false;
		if ( this.firedPosition.x != w.firedPosition.x )
			return false;
		if ( this.firedPosition.y != w.firedPosition.y )
			return false;

		return true;

	}

	public safetyCorridor getSafetyCorridor( firingSolution fS) {
		// safety corridor made by a bullet shadow from firingSolution fS.
		safetyCorridor sC = null;

		long t = Math.max( firedTime, fS.getFiredTime());
		// slow bullets are still inside a bot for 1st 2 tics 
		// thus they can be intercepted
		t = t + 2; 
		double d = 0;
		double dist = firedPosition.distance( fS.getLocationAt ( t ) ) - getDistanceTraveledAtTime(t);
		if ( dist > 0 ) {
			long tmax = t+50; // 1000px/20pxPerTic should be good enough
			double distPrev = Double.NEGATIVE_INFINITY;
			do {
				t++ ;
				distPrev = dist;
				dist = firedPosition.distance( fS.getLocationAt ( t ) ) - getDistanceTraveledAtTime(t);
			} while (dist > 0 && (dist < distPrev) && (t < tmax) );
			if ( (dist < 0) && (0 < distPrev) ) {
				// firing solution was approaching wave front
				// and intersected it in between two points below
				Point2D.Double fsPos1 = fS.getLocationAt ( t );
				Point2D.Double fsPos2 = fS.getLocationAt ( t-1 );

				if ( 0 > ( firedPosition.distance( fsPos1 ) - getDistanceTraveledAtTime(t-1) ) ) {
					// firing solution traveled beyond wave at t-1
					// we need to correct intersection point
					fsPos1 = intersectWithLine( t-1, fsPos1, fsPos2 );
				}

				if ( 0 < ( firedPosition.distance( fsPos2 ) - getDistanceTraveledAtTime(t) ) ) {
					// firing solution was out of reach for wave at t
					// we need to correct intersection point
					fsPos2 = intersectWithLine( t, fsPos1, fsPos2 );
				}


				double hitAngle1 = math.angle2pt( firedPosition, fsPos1 );
				double hitAngle2 = math.angle2pt( firedPosition, fsPos2 );

				sC = new safetyCorridor( hitAngle1, hitAngle2);
			} 
		}
		return sC; 
	}

	public Point2D.Double intersectWithLine( long t, Point2D.Double LineEnd1, Point2D.Double LineEnd2) {
		double R = getDistanceTraveledAtTime(t);
		// we will do bisection search
		double dist;
		// assigning proper ends for bisection
		Point2D.Double posEnd = null, negEnd = null;
		dist = R - firedPosition.distance( LineEnd1 );
		if ( dist > 0 ) {
			posEnd = (Point2D.Double) LineEnd1.clone();
		} else {
			negEnd = (Point2D.Double) LineEnd1.clone();
		}
		dist = R - firedPosition.distance( LineEnd2 );
		if ( dist > 0 ) {
			posEnd = (Point2D.Double) LineEnd2.clone();
		} else {
			negEnd = (Point2D.Double) LineEnd2.clone();
		}
		if ( posEnd == null || negEnd == null ) {
			logger.error("Error: there is no solution for bisection intersect");
			return LineEnd1;
		}
		double eps = 1e-3;
		int Ntrials = 100;
		Point2D.Double testPos = null;
		while ( posEnd.distance( negEnd ) > eps && Ntrials > 0 ) {
			Ntrials--;
			testPos = new Point2D.Double(
					(LineEnd2.getX() + LineEnd1.getX()) / 2,
					(LineEnd2.getY() + LineEnd1.getY()) / 2
					);
			dist = R - firedPosition.distance( testPos );
			if ( dist > 0 ) {
				posEnd = testPos;
			} else {
				negEnd = testPos;
			}
		}

		return testPos;
	}

	public safetyCorridor getSafetyCorridor( fighterBot bot) {
		Point2D.Double pos = bot.getPosition();
		return getSafetyCorridor( pos );
	}

	public safetyCorridor getSafetyCorridor( Point2D.Double pos ) {
		double hitAngle = math.angle2pt( firedPosition, pos );
		double dist = firedPosition.distance( pos );
		double shadowHalfAngle = Math.atan(physics.robotHalfDiagonal/dist);
		shadowHalfAngle = Math.toDegrees( shadowHalfAngle );
		safetyCorridor sC = new safetyCorridor( hitAngle - shadowHalfAngle, hitAngle + shadowHalfAngle );
		return sC;
	}

	public void addSafetyCorridor( fighterBot bot) {
		safetyCorridor sC = getSafetyCorridor( bot );
		addToSafetyCorridors(sC);
	}

	public void addToSafetyCorridors( safetyCorridor sC ) {
		if ( sC.getCorridorSize() < safetyCorridorSizeThreshold ) {
			return;
		}
		boolean haveJoin = false;
		safetyCorridor sCjoin = null;
		safetyCorridor toRemove = null;
		for( safetyCorridor c : safetyCorridors ) {
			if ( c.getOverlap(sC) != null ) {
				haveJoin = true;
				sCjoin = c.getJoin(sC);
				toRemove = c;
				break;
			}
		}
		if ( haveJoin ) {
			safetyCorridors.remove( toRemove );
			addToSafetyCorridors( sCjoin );
		} else {
			safetyCorridors.add(sC);
		}
	}

	public void drawSafetyCorridor(Graphics2D g, safetyCorridor sC, long time) {
		double distTraveled = getDistanceTraveledAtTime( time );
		g.setColor( new Color(0x00, 0xff, 0x00, 0x80) );
		// strangely drawCircArc uses games coordinates
		double minA = ( sC.getMinAngle() );
		double maxA = ( sC.getMaxAngle() );
		graphics.drawCircArc( g, firedPosition, distTraveled, minA, maxA );
		graphics.drawCircArc( g, firedPosition, distTraveled+1, minA, maxA );
		graphics.drawCircArc( g, firedPosition, distTraveled-1, minA, maxA );
	}

	public void onPaint(Graphics2D g, long timeNow) {
		g.setColor(waveColor);

		// draw overall  wave
		double distTraveled = getDistanceTraveledAtTime( timeNow );
		graphics.drawCircle(g, firedPosition, distTraveled);
		if ( timeUncertaintyLower !=  0 ) {
			g.setColor(waveUncertaintyColorLower);
			graphics.drawCircle(g, firedBot.getPositionAtTime(firedTime - timeUncertaintyLower), distTraveled + bulletSpeed * timeUncertaintyLower);
		}
		for ( safetyCorridor sC : safetyCorridors ) {
			drawSafetyCorridor(g, sC, timeNow);
		}
	}
}
