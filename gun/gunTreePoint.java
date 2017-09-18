// -*- java -*-

package eem.frame.gun;

import java.awt.geom.Point2D;
import eem.frame.bot.*;
import eem.frame.misc.*;

import robocode.*;

public class gunTreePoint  {
	protected int kdTreeDims = 8; // dist, bulletEnergy, abs(latVel), accel, dist to wall, enemy num, timeSinceVelocityChange, advancing speed
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
		double advancingSpeed = tBStat.getAdvancingSpeed( fPos );
		double latteralSpeed = tBStat.getLateralSpeed( fPos );
		double speed         = Math.abs( tBStat.getSpeed() );
		double latteralSpeedPrev = tBStatPrev.getLateralSpeed( fPos );

		double accel = (latteralSpeed - latteralSpeedPrev)/(tBStat.getTime() - tBStatPrev.getTime());
		if ( Double.isNaN( accel) ) {
			accel = 0;
		}
		accel = math.signNoZero( latteralSpeed )*accel; // speed up or slow down

		double distToWallAhead = tBStat.getDistanceToWallAhead();

		long timeSinceVelocityChange = tBStat.getTimeSinceVelocityChange();

		double vBullet = physics.bulletSpeed( bulletEnergy ),
		       vBulletMax=physics.bulletSpeed( robocode.Rules.MIN_BULLET_POWER );
		double vBotMax = robocode.Rules.MAX_VELOCITY;
		double tFlight = distAtLastAim/(vBullet+advancingSpeed);
		double tWallHit = distToWallAhead;
		double MEA =    physics.calculateMEA( vBullet );
		double posMEA = physics.calculateConstrainedMEA( vBullet, fPos, tPos, true);
		double negMEA = physics.calculateConstrainedMEA( vBullet, fPos, tPos, false);

		//logger.dbg( tBot.getName() + " has maxMEA = " + physics.calculateMEA(vBullet) + " negMEA = " + negMEA + " posMEA = " + posMEA + " and laterals speed = " + latteralSpeed );

		// assign normalized coordinates
		double x; // dummy variable
		x = distAtLastAim/physics.MaxSeparationOnBattleField;
		coord[0] = advancingSpeed;
		coord[1] = latteralSpeed;
		coord[2] = 1.1*tFlight;
		coord[3] = 1.0*1.0/(1+Math.min(distToWallAhead/vBotMax, tWallHit));
		coord[4] = 100*posMEA/MEA;
		coord[5] = 100*negMEA/MEA;
		x = timeSinceVelocityChange;
		coord[6] = Math.max(x,10);
		coord[7] = 1/(1 + Math.max(0,(fBot.getEnemyBots().size()-1)) ); //max to avoid division by zero if the bot win the battle

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
