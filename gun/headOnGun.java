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
		color = new Color(0x00, 0x00, 0x00, 0xff);
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		Point2D.Double fP = fBot.getMotion().getPositionAtTime( time );
		return getFiringSolutions( fP, tBot, time, bulletEnergy);
	}

	public LinkedList<firingSolution> getFiringSolutions( Point2D.Double fP, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();

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
		fS.setQualityOfSolution( getLagTimePenalty( infoLagTime ) );

		fSolultions.add(fS);
		return fSolultions;

	}
	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		Point2D.Double fP = fBot.getPositionClosestToTime( time );
		return getFiringSolutions( fP, tBot, time, bulletEnergy);
	}

}

