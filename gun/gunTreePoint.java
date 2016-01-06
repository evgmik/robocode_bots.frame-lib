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

		// assign normilized coordinates
		coord[0] = distAtLastAim*10/physics.MaxSeparationOnBattleField;
		coord[1] = bulletEnergy/robocode.Rules.MAX_BULLET_POWER;
		coord[2] = Math.abs(latteralSpeed)*10/robocode.Rules.MAX_VELOCITY;
		coord[3] = math.signNoZero( latteralSpeedPrev)*accel;
		coord[4] = distToWallAhead*10/physics.MaxSeparationOnBattleField;
		coord[5] = fBot.getEnemyBots().size()*2;
		coord[6] = math.putWithinRange( Math.sqrt( timeSinceVelocityChange ), 0, 10 )/10;
	}
}
