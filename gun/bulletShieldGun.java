// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class bulletShieldGun extends headOnGun {
	// this gun shuts bullet in a hope to provide maximum bullet shadow
	// this way bot can hide in the bullet shadow and decrease chances of being hit
	public bulletShieldGun() {
		gunName = "bulletShieldGun";
		color = new Color(0x00, 0xFF, 0x00, 0xff);
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		Point2D.Double fP = fBot.getMotion().getPositionAtTime( time );
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();

		if (fP == null)
			return fSolultions;
		botStatPoint fBStat = fBot.getStatClosestToTime( time  );

		// if some one fires at 'time',
		// the latest time, when target stats are known, is at 'time-1'
		botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
		if (tBStat == null)
			return fSolultions;

		Point2D.Double tP = tBStat.getPosition( );
		if (tP == null)
			return fSolultions;

		double headOnAngle = math.angle2pt( fP, tP );
		double bSpeed = physics.bulletSpeed( bulletEnergy );
		// the lager lateral projection the better, it gives maximum shielding zone.
		// From other hand, lateral projection of the bullet speed should not exceed
		// maximum bot speed, otherwise bot cannot catch up with safety zone.
		// Technically we should compare with maximum 
		// lateral speed of our bot at the fire time.
		double lateralSpeed = fBStat.getLateralSpeed( tP );
		// give a slight preference to faster motion, it also increases shadow
		lateralSpeed = 1.2*lateralSpeed; 
		lateralSpeed = math.putWithinRange(
			       	-robocode.Rules.MAX_VELOCITY, 
				lateralSpeed, 
				robocode.Rules.MAX_VELOCITY );
		double offsetAngle = Math.toDegrees( Math.asin( (-lateralSpeed)/bSpeed ) );
		double firingAngle = headOnAngle + offsetAngle;

		firingSolution fS = new firingSolution( this, fP, firingAngle, time, bulletEnergy );

		setDistanceAtLastAimFor( fS, fP, tP );
		//fS.setQualityOfSolution( getLagTimePenalty( 0 ) ); // the best
		fS.setQualityOfSolution( 1 ); // the best
		//fS = correctForInWallFire(fS);
		fSolultions.add(fS);
		fSolultions = setTargetBotName( tBot.getName(), fSolultions );
		return fSolultions;

	}

}

