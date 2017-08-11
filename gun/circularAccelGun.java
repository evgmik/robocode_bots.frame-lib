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
		//logger.routine(tBot.getName() + " heading in rads " + tBStat.getHeadingRadians() + " speed " + tBStat.getSpeed() + " velocity " + vTvec);

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
			double phiLast=0, phiPrev=0;
			if ( tBStat.getSpeed() != 0 ) {
				phiLast = Math.atan2( vTvec.y, vTvec.x);
				phiLast = Math.toDegrees( phiLast );
			} else {
				phiLast = math.game_angles2cortesian( tBStat.getHeadingDegrees() );
				phiLast = math.shortest_arc( phiLast );
			}
			if ( bStatPrev.getSpeed() != 0 ) {
				phiPrev = Math.atan2( vTvecPrev.y, vTvecPrev.x);
				phiPrev = Math.toDegrees( phiPrev );
			} else {
				phiPrev = math.game_angles2cortesian( bStatPrev.getHeadingDegrees() );
				phiPrev = math.shortest_arc( phiPrev );
			}
			double dt =  tBStat.getTime() - bStatPrev.getTime();
			if ( dt == 0 ) {
				// previous point is the same as current
				phi = 0; // falling back to linear gun
				accel = 0; // no way to know acceleration
				//return fSolultions; // circular gun is not applicable
			} else {
				phi = (phiLast - phiPrev)/dt; // it is actually angular velocity
				phi = math.shortest_arc( phi );
				if ( Math.abs( phi) > 90 ) {
					// probably we had a direction flip
					// let's convert the angle to take it in account
					if ( phi > 0 ) {
						phi = - (180 - phi);
					} else {
						phi = 180 + phi;
					}
				}
				double eps=1e-6;
				if ( Math.abs( phi ) >  (robocode.Rules.MAX_TURN_RATE+eps) ) {
					// Most likely our speed was zero and now we start moving
					// in quite different direction from previous one.
					// Alternatively, we just hit a wall, which looks like velocity flip
					// Forcing rotation to 0.
					logger.error("Something wrong: rotation rate " + phi + "  is to high forcing it to 0. phiLast=" + phiLast + " phiPrev=" + phiPrev);
					phi = 0;

				}
				phi = Math.toRadians( phi );
				double speedLast = vTvec.distance(0,0);
				double speedPrev = vTvecPrev.distance(0,0);
				accel = (speedLast - speedPrev)/dt;
				if  ( (accel < -robocode.Rules.DECELERATION) && (speedLast == 0) ) {
					// collision with wall or other bot detected
					//logger.routine("Collision detected dropping acceleration");
					accel = 0;
				}
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

