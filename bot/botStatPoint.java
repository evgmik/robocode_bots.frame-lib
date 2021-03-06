// -*- java -*-

package eem.frame.bot;

import eem.frame.core.*;
import eem.frame.misc.*;

import robocode.*;
import robocode.Rules.*;
import java.awt.geom.Point2D;

import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

public class botStatPoint {
	private Point2D.Double pos;
	private long tStamp;
	private double headingDegrees;
	private double speed;
	private double energy;
	private double gunHeat;
	private double dist2WallAhead;
	private long timeSinceVelocityChange;

	public botStatPoint() {
		pos = new Point2D.Double(0,0);
		tStamp = 0;
		headingDegrees = 0;
		speed = 0;
		energy =0;
		gunHeat =0;
		dist2WallAhead=0;
		timeSinceVelocityChange = 0;
	}

	public botStatPoint(AdvancedRobot bot, ScannedRobotEvent e ) {
		this();
		Point2D.Double myCoord = new Point2D.Double();
		myCoord.x = bot.getX();
	       	myCoord.y = bot.getY();
		double angle = Math.toRadians(bot.getHeading()+ e.getBearing());
		double distance = e.getDistance();
		pos = new Point2D.Double( (myCoord.x + Math.sin(angle) * distance),
				(myCoord.y + Math.cos(angle) * distance) );
		tStamp = bot.getTime();
		headingDegrees = e.getHeading();
		speed = e.getVelocity();
		energy = e.getEnergy();

		dist2WallAhead = distanceToWallAhead(this);
		timeSinceVelocityChange = 0; // no way to know it with in this class

		//logger.dbg("bot stat = " + this.format() );
	}

	public botStatPoint(CoreBot bot) {
		this();
		pos.x = bot.getX();
	       	pos.y = bot.getY();
		tStamp = bot.getTime();
		headingDegrees = bot.getHeading();
		speed = bot.getVelocity();
		energy = bot.getEnergy();
		dist2WallAhead = distanceToWallAhead();
		timeSinceVelocityChange = 0; // no way to know it with in this class
		//logger.dbg("Master bot stat = " + this.format() );
	}

	public botStatPoint(Point2D.Double p, long t ) {
		this();
		tStamp = t;
		pos = p;
	}

	public double getDistanceToWallAhead() {
		return dist2WallAhead;
	}

	public void setTimeSinceVelocityChange(long t) {
		timeSinceVelocityChange = t;
	}

	public long getTimeSinceVelocityChange() {
		return timeSinceVelocityChange;
	}

	public double getDistance(Point2D.Double p) {
		return p.distance(pos);
	}

	public String format() {
		String str = "";
		str += "tStamp = " + tStamp;
		str += ", ";
		str += "position = ["
			+ logger.shortFormatDouble( pos.x ) 
			+ ", "
			+ logger.shortFormatDouble( pos.y ) 
	      		+ "]";
		str += ", ";
		str += "energy = " + logger.shortFormatDouble(energy);
		str += ", ";
	       	str += "speed = " + logger.shortFormatDouble(speed);
		str += ", ";
	       	str += "heading = " + logger.shortFormatDouble(headingDegrees);
		str += ", ";
		str += "distance to " + whichWallAhead() +" wall ahead = " + logger.shortFormatDouble( getDistanceToWallAhead() );
		return str;
	}

	public long getTime() {
		return tStamp;
	}

	public void setTime(long t) {
		tStamp = t;
	}

	public double getEnergy() {
		return energy;
	}
	public double getX() {
		return pos.x;
	}

	public double getY() {
		return pos.y;
	}
	
	public double getHeadingDegrees() {
		return headingDegrees;
	}

	public void setHeadingDegrees(double angle) {
		headingDegrees = angle;
	}


	public double getHeadingRadians() {
		return Math.toRadians( headingDegrees );
	}

	public double getSpeed() {
		return speed;	
	}

	public void setSpeed( double s ) {
		speed = s;
	}

	public Point2D.Double getVelocity() {
		Point2D.Double velocity = new Point2D.Double( speed*Math.sin( Math.toRadians(headingDegrees) ), speed*Math.cos( Math.toRadians(headingDegrees) ) );
		return velocity;
	}

	public double getLateralSpeed( Point2D.Double p) {
		// lateral speed with respect to point p
		double absoluteBearing = math.angle2pt( p, pos );
		double lateralSpeed = speed * Math.sin( Math.toRadians(headingDegrees - absoluteBearing) );
		// positive means: enemy is circling clockwise
		return lateralSpeed;
	}

	public double getAdvancingSpeed( Point2D.Double p) {
		// lateral speed with respect to point p
		double absoluteBearing = math.angle2pt( p, pos );
		double advancingSpeed = -speed * Math.cos( Math.toRadians(headingDegrees - absoluteBearing) );
		// positive means: enemy is moving to the point p
		return advancingSpeed;
	}

	public Point2D.Double getPosition() {
		return (Point2D.Double) pos.clone();
	}

	public void setPosition( Point2D.Double p) {
		 pos = (Point2D.Double) p.clone();
	}

	public double getGunHeat() {
		return gunHeat;
	}

	public boolean arePointsOfPathSimilar(botStatPoint refPatStart, botStatPoint refPatCurrent, botStatPoint testPatStart) {
		//profiler.start( "arePointsOfPathSimilar" );
		// essentially does this point matches refPatCurrent point.
		// compare how this stat point with respect to testPatStart
		// matches reference Start and refPatCurrent
		double dist2WallProximity = 80;
		double dist2WallDiff = 4;
		double maxMyBotGunHeatDist = 0.2;
		double maxSpeedDist = 0.5;
		double maxDistDist = 50;
		double maxLateralDist = 2;
		double maxAngleDist = 10; // 10 is maximum bot rotation per turn
		double spdT = this.getSpeed();
		double angT = this.getHeadingDegrees() - testPatStart.getHeadingDegrees();
		long   timeDiffT = this.getTime() - testPatStart.getTime();
		double dist2wallAheadT = this.getDistanceToWallAhead();

		double spdR = refPatCurrent.getSpeed();
		double angR = refPatCurrent.getHeadingDegrees() - refPatStart.getHeadingDegrees();
		long   timeDiffR = refPatCurrent.getTime() - refPatStart.getTime();
		double dist2wallAheadR = refPatCurrent.getDistanceToWallAhead();
		boolean areSimilar = true;

		SameOrDifferent: {
			if (
				( Math.abs( spdT - spdR ) > maxSpeedDist )
				|| ( Math.abs( math.shortest_arc( angT - angR) ) > maxAngleDist )
				//|| ( Math.abs( dist2myBotT - dist2myBotR) > maxDistDist )
			   ) {
				areSimilar = false;
				break SameOrDifferent;
			}
			// now let's check that the timing is right
			if ( timeDiffT != timeDiffR ) {
				areSimilar = false;
				break SameOrDifferent;
			}

			if ( false ) { // disable heat comparison
			if ( Math.min( this.getGunHeat(), refPatCurrent.getGunHeat() ) < .5) {
				if ( Math.abs( this.getGunHeat()  - refPatCurrent.getGunHeat() ) > maxMyBotGunHeatDist ) {
					areSimilar = false;
					break SameOrDifferent;
				}
			}
			}

			if ( Math.min( dist2wallAheadR, dist2wallAheadT) < dist2WallProximity ) {
				if ( Math.abs( dist2wallAheadT - dist2wallAheadR ) > dist2WallDiff ) {
					areSimilar = false;
					break SameOrDifferent;
				}
			}
			//logger.dbg("---- The following two bot stats match each other");
			//logger.dbg( this.format() );
			//logger.dbg( refPatCurrent.format() );
		} // end of SameOrDifferent
		//profiler.stop( "arePointsOfPathSimilar" );
		return areSimilar;
	}

	public String whichWallAhead(botStatPoint bStatPnt) {
		return physics.whichWallAhead( bStatPnt.getPosition(), bStatPnt.getSpeed(), bStatPnt.getHeadingRadians() );
	}

	public String whichWallAhead() {
		return whichWallAhead( this );
	}

	public double distanceToWallAhead(botStatPoint bStatPnt) {
		return physics.distanceToWallAhead( bStatPnt.getPosition(), bStatPnt.getSpeed(), bStatPnt.getHeadingRadians() );
	}

	public double distanceToWallAhead() {
		return distanceToWallAhead( this );
	}
}
