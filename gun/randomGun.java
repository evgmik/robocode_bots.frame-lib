// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class randomGun extends baseGun {
	public randomGun() {
		gunName = "randomGun";
		color = new Color(0xff, 0xff, 0xff, 0x80);
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		Point2D.Double fP = fBot.getMotion().getPositionAtTime( time );
		LinkedList<firingSolution> fSols = getFiringSolutions( fP, tBot, time, bulletEnergy);
		fSols = setFiringBotName( fBot.getName(), fSols );
		return fSols;
	}

	public LinkedList<firingSolution> getFiringSolutions( Point2D.Double fP, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();

		if (fP == null)
			return fSolultions;

		// if some one fires at 'time',
		// the latest time, when target stats are known, is at 'time-1'
		botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
		if (tBStat == null)
			return fSolultions;

		Point2D.Double tP = tBStat.getPosition( );
		if (tP == null)
			return fSolultions;

		
		// now lets find random escape angle
		// angle to target, robocode angle degrees
		double angle = math.angle2pt(fP, tP);
		double dist = fP.distance(tP); // distance to target
		double bSpeed = physics.bulletSpeed( bulletEnergy );
		double maxEscapeAngle = Math.atan( robocode.Rules.MAX_VELOCITY/ bSpeed); 
		double fireOffsetAngle = Math.toDegrees( 2*( Math.random()-0.5 ) * maxEscapeAngle );
		angle = angle + fireOffsetAngle;

		// random gun does not work with future target position but with angle
		firingSolution fS = new firingSolution( this, fP, angle, time, bulletEnergy );

		long infoLagTime = time - tBStat.getTime(); // ideally should be 0
		fS.setQualityOfSolution( getLagTimePenalty( infoLagTime ) );

		fSolultions.add(fS);
		fSolultions = setTargetBotName( tBot.getName(), fSolultions );
		return fSolultions;

	}
	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		Point2D.Double fP = fBot.getPositionClosestToTime( time );
		LinkedList<firingSolution> fSols = getFiringSolutions( fP, tBot, time, bulletEnergy);
		fSols = setFiringBotName( fBot.getName(), fSols );
		return fSols;
	}

}

