// -*- java -*-

package eem.frame.gun;

import java.awt.geom.Point2D;
import eem.frame.bot.*;
import eem.frame.misc.*;

import robocode.*;

public class misc  {

	public static Point2D.Double linear_predictor( double bSpeed, Point2D.Double tgt_pos, Point2D.Double vTvec, Point2D.Double myBot_pos) {
		double tFX, tFY; // target future position
		double Tx, Ty, vT,  dx, dy, dist;
		AdvancedRobot bot = new AdvancedRobot(); // just to get battlefield parameters
		double timeToHit;
		double a, b, c;

		// radius vector to target
		dx = tgt_pos.x-myBot_pos.x;
		dy = tgt_pos.y-myBot_pos.y;
		dist = Math.sqrt(dx*dx + dy*dy);

		// rough estimate
		// use it for better estimate of possible target future velocity
		timeToHit = dist/bSpeed;

		vT=vTvec.distance(0,0);

		// back of envelope calculations
		// for the case of linear target motion with no acceleration
		// lead to quadratic equation for time of flight to target hit
		a = vT*vT - bSpeed*bSpeed;
		b = 2*( dx*vTvec.x + dy*vTvec.y);
		c = dist*dist;

		// FIXME avoid precise solver and just do it iteratively.
		timeToHit = math.quadraticSolverMinPosRoot( a, b, c);
		tFX = (int) ( tgt_pos.x + vTvec.x*timeToHit );
		tFY = (int) ( tgt_pos.y + vTvec.y*timeToHit );

		int cnt = 400; // safety count
		Point2D.Double tFpos = new Point2D.Double( tFX, tFY );
		while ( !physics.botReacheableBattleField.contains( tFpos ) ) {
			tFpos.x = tFpos.x - vTvec.x;
			tFpos.y = tFpos.y - vTvec.y;
			cnt -= 1;
			if ( cnt <= 0 ) {
				logger.error("linear gun cannot converge");
				logger.error(" target position is = " + tFpos);
				break;
			}
		}

		return tFpos;
	}

	public static double[] calcTreePointCoord( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy, int coordVecSize ) {
		// time is time at fire
		double[] coord = new double[coordVecSize];

		Point2D.Double fPos = fBot.getInfoBot().getPositionAtTime( time );
		if ( fPos == null ) {
			fPos = fBot.getMotion().getPositionAtTime( time );
		}
		if ( fPos == null) {
			logger.error( "error: unable to find fPos for bot " + fBot.getName() + "at time " + (time) );
			return coord;
		}
		// the latest time, when target stats are known, is at 'time-1'
		botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
		if (tBStat == null) {
			logger.error( "error: unable to find tBStat at time " + (time-1) );
		}
		Point2D.Double tPos = tBStat.getPosition();

		double distAtLastAim = fPos.distance( tPos );
		double latteralSpeed = Math.abs( tBStat.getLateralSpeed( fPos ) );

		// assign normilized coordinates
		coord[0] = distAtLastAim/physics.BattleField.distance(0,0);
		coord[1] = bulletEnergy/robocode.Rules.MAX_BULLET_POWER;
		coord[2] = latteralSpeed/robocode.Rules.MAX_VELOCITY;
		return coord;
	}
}
