// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class circularAccelGun extends circularGun {
	// circular gun with acceleration
	public circularAccelGun() {
		gunName = "circularAccelGun";
		color = new Color(0xff, 0x00, 0x99, 0x80);
	}
	// FIXME: Join this gun with circular one. They differ only in the use or no use
	// of acceleration. And this gun has a more general algorithm

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

		// position previous to fire time
		botStatPoint bStatPrev = tBot.getStatClosestToTime( time - 2 );

		double phi = 0;
		double accel = 0;
		Point2D.Double posFut  = new Point2D.Double(0,0);
		Point2D.Double vTvecLast, vTvecPrev;

		if ( bStatPrev == null ) {
			phi = 0; // falling back to linear gun
			accel = 0;
			//return fSolultions; // circular gun is not applicable
		} else {
			vTvecPrev = bStatPrev.getVelocity();
			double phiLast = Math.atan2( vTvec.y, vTvec.x);
			double phiPrev = Math.atan2( vTvecPrev.y, vTvecPrev.x);
			double dt =  tBStat.getTime() - bStatPrev.getTime();
			if ( dt == 0 ) {
				// previous point is the same as current
				phi = 0; // falling back to linear gun
				accel = 0; // no way to know acceleration
				//return fSolultions; // circular gun is not applicable
			} else {
				phi = (phiLast - phiPrev)/dt;
				double speedLast = vTvec.distance(0,0);
				double speedPrev = vTvecPrev.distance(0,0);
				accel = (speedLast - speedPrev)/dt;
				accel = math.putWithinRange( accel, -robocode.Rules.DECELERATION,  robocode.Rules.ACCELERATION );
			}
		}

		posFut = getProjectedMotionMeetsBulletPosition( tBStat, phi, accel, fPos, time, bSpeed );
		firingSolution fS = new firingSolution( this, fPos, posFut, time, bulletEnergy );
		setDistanceAtLastAimFor( fS, fPos, tPos );
		long infoLagTime = time - tBStat.getTime(); // ideally should be 0
		fS.setQualityOfSolution( getLagTimePenalty( infoLagTime ) );
		fS = correctForInWallFire(fS);
		fSolultions.add(fS);
		fSolultions = setTargetBotName( tBot.getName(), fSolultions );
		return fSolultions;
	}
}

