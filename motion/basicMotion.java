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



public class basicMotion {
	protected fighterBot myBot;
	public boolean needToRecalculate = true;
	public long predictionEndTime=0;
	public dangerPoint destPoint = null;

	public void initTic() {
	}

	public basicMotion() {
	}

	public basicMotion(fighterBot bot) {
		initBattle( bot );
	}

	public void initBattle(fighterBot b) {
		myBot = b;
	}

	public void moveToPoint( Point2D.Double pnt ) {
		driveCommand _driveCommand = pathSimulator.moveToPointDriveCommand( myBot.getPosition(), myBot.getHeadingDegrees(), pnt );
		setTurnRight(_driveCommand.turnRightAngleDegrees);
		setAhead (_driveCommand.moveAheadDist);
	}

	public Point2D.Double getPositionAtTime(long time) {
		if ( destPoint == null ) {
			// this is trigered by enemy bots, we do not know their path
			// intension
			return myBot.getPositionClosestToTime( time );
		} else {
			// master bot should know desired point
			if ( myBot.getTime() >= time ) {
				// the point is in the past or present
				return myBot.getPositionClosestToTime( time );
			} else {
				// the required point is in the future
				// we will try to gess it

				// a bit of safety net
				long lastSeenTime = myBot.getLastSeenTime();
				Point2D.Double myPos = (Point2D.Double) myBot.getPositionClosestToTime( lastSeenTime ).clone();
				LinkedList<botStatPoint> pathToDest = pathSimulator.getPathTo( destPoint.getPosition(), myBot.getStatClosestToTime( lastSeenTime ), time - lastSeenTime );
				return pathToDest.getLast().getPosition();
			}
		}
	}

	public void setTurnRight( double angle) {
		myBot.proxy.setTurnRight( angle );
	}

	public void setAhead( double dist ) {
		myBot.proxy.setAhead( dist);
	}

	public void manage() {
		// for basic motion we do nothing
	}

	public void makeMove() {
		// for basic motion we do nothing
	}

	public void choseNewPath( long ticsCount ) {
		// find new path for the following ticsCount
	}

	public void onPaint(Graphics2D g) {
	}

}
