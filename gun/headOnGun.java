// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class headOnGun extends baseGun {
	public headOnGun() {
		gunName = "headOnGun";
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();
		Point2D.Double fP = fBot.getPositionClosestToTime( time );
		if (fP == null)
			return fSolultions;

		botStatPoint tBStat = tBot.getStatClosestToTime( time );
		if (tBStat == null)
			return fSolultions;

		Point2D.Double tP = tBStat.getPosition( );
		if (tP == null)
			return fSolultions;

		firingSolution fS = new firingSolution( this, fP, tP, time, bulletEnergy );

		long infoLagTime = time - tBStat.getTime(); // ideally should be 0
		if ( infoLagTime <= 0  ) {
			// time point from the future
			fS.setQualityOfSolution(1); // 1 is the best
		}
		if ( infoLagTime > 0  ) {
			// we are using outdated info
			fS.setQualityOfSolution( Math.exp(infoLagTime/5) );
		}

		fSolultions.add(fS);
		return fSolultions;
	}

}

