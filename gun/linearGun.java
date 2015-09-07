// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class linearGun extends baseGun {
	public linearGun() {
		gunName = "linearGun";
		color = new Color(0xff, 0x00, 0x00, 0xff);
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		Point2D.Double fPos = fBot.getMotion().getPositionAtTime( time );
		return getFiringSolutions( fPos, tBot, time, bulletEnergy);
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();

		botStatPoint fBStat = fBot.getStatClosestToTime( time );
		if (fBStat == null)
			return fSolultions;
		Point2D.Double fPos = (Point2D.Double) fBStat.getPosition().clone();

		return getFiringSolutions( fPos, tBot, time, bulletEnergy);
	}

	public LinkedList<firingSolution> getFiringSolutions( Point2D.Double fPos, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();
		if (fPos == null )
			return fSolultions;

		// the latest time, when target stats are known, is at 'time-1'
		botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
		if (tBStat == null)
			return fSolultions;

		// OK we know fire point and at least one target position
		Point2D.Double tPos = (Point2D.Double) tBStat.getPosition().clone();
		Point2D.Double vTvec = (Point2D.Double) tBStat.getVelocity().clone();

		double bSpeed = physics.bulletSpeed( bulletEnergy );

		// where target will be at wave intersection according to linear predictor
		Point2D.Double tFPos = misc.linear_predictor( bSpeed, tPos, vTvec,  fPos);

		firingSolution fS = new firingSolution( this, fPos, tFPos, time, bulletEnergy );

		long infoLagTime = time - tBStat.getTime(); // ideally should be 0
		if ( infoLagTime <= 0  ) {
			// time point from the future
			fS.setQualityOfSolution(1); // 1 is the best
		}
		if ( infoLagTime > 0  ) {
			// we are using outdated info
			fS.setQualityOfSolution( Math.exp(infoLagTime/5) );
			logger.noise("time " + time + " target info is outdated by " + infoLagTime);
		}
		// check if the future target point is within botReacheable space
		if ( physics.shortestDist2wall( tFPos ) < physics.robotHalfSize ) {
			logger.noise("time " + time + " unphysical future target positon");
			fS.setQualityOfSolution( 0 ); // bad solution
		}

		logger.noise("linear gun firingSolution: " + fS.toString());
		fSolultions.add(fS);
		return fSolultions;
	}
}

