// -*- java -*-

package eem.frame.gun;

import java.awt.geom.Point2D;
import eem.frame.bot.*;
import eem.frame.misc.*;

import robocode.*;

public class gunTreePoint  {
	protected int kdTreeDims = 7; // dist, bulletEnergy, abs(latVel), accel, dist to wall, enemy num, timeSinceVelocityChange
	protected double[] coord = new double[kdTreeDims];

	public int getKdTreeDims() {
		return kdTreeDims;
	}

	public double[] getPosition() {
		return coord;
	}

	public gunTreePoint() {
	}

	public gunTreePoint( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		// time is time at fire

		Point2D.Double fPos = fBot.getInfoBot().getPositionAtTime( time );
		if ( fPos == null ) {
			fPos = fBot.getMotion().getPositionAtTime( time );
		}
		if ( fPos == null) {
			logger.error( "error: unable to find fPos for bot " + fBot.getName() + "at time " + (time) );
			return;
		}
		// the latest time, when target stats are known, is at 'time-1'
		botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
		if (tBStat == null) {
			logger.error( "error: unable to find tBStat at time " + (time-1) );
		}
		Point2D.Double tPos = tBStat.getPosition();

		botStatPoint tBStatPrev = tBot.getStatClosestToTime( time - 2 );

		double distAtLastAim = fPos.distance( tPos );
		double bulletFlightTime = distAtLastAim/physics.bulletSpeed( bulletEnergy );
		double latteralSpeed = tBStat.getLateralSpeed( fPos );
		double latteralSpeedPrev = tBStatPrev.getLateralSpeed( fPos );

		double accel = (latteralSpeed - latteralSpeedPrev)/(tBStat.getTime() - tBStatPrev.getTime());
		if ( Double.isNaN( accel) ) {
			accel = 0;
		}

		double distToWallAhead = tBStat.getDistanceToWallAhead();

		long timeSinceVelocityChange = tBStat.getTimeSinceVelocityChange();

		// assign normalized coordinates
		double x; // dummy variable
		x = distAtLastAim/physics.MaxSeparationOnBattleField;
		coord[0] = x*x;
		coord[1] = bulletEnergy/robocode.Rules.MAX_BULLET_POWER;
		coord[2] = Math.abs(latteralSpeed)/robocode.Rules.MAX_VELOCITY;
		x = math.signNoZero( latteralSpeed )*(accel/2); // -1 to 1
		coord[3] = 1/2. + 1/2.*x;
		x = distToWallAhead/robocode.Rules.MAX_VELOCITY;
		coord[4] = 1/(1+x);
		coord[5] = 1/(1 + Math.max(0,(fBot.getEnemyBots().size()-1)) ); //max to avoid division by zero if the bot win the battle
		x = timeSinceVelocityChange;
		coord[6] = 1/(1+x);

		if ( false ) { // enable for debugging
			String sout = fBot.getName() + " Tree coords: ";
			sout += logger.arrayToTextPlot( coord ) + " --> ";
			for(int i=0; i< kdTreeDims; i++) {
				sout += Math.round(100*coord[i]);
				sout += ", ";
			}
			logger.dbg( sout);
		}
	}
}
