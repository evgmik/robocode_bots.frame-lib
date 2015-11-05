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
		fSolultions.add(fS);
		fSolultions = setTargetBotName( tBot.getName(), fSolultions );
		return fSolultions;
	}

	// wave interception predictor for circular motion with acceleration
	public Point2D.Double getProjectedMotionMeetsBulletPosition( botStatPoint lastSeenStatPoint, double botBodyRotationSpeed, double botAcceleration, Point2D.Double firingPosition, long timeAtFiring , double bSpeed ) {

		Point2D.Double posFut = (Point2D.Double) lastSeenStatPoint.getPosition().clone();
		Point2D.Double vTvec = (Point2D.Double) lastSeenStatPoint.getVelocity().clone();
		double vx = vTvec.x;
		double vy = vTvec.y;
		double speed = vTvec.distance(0,0);
		double phi = Math.atan2( vTvec.y, vTvec.x);

		double vxNew, vyNew;

		// note, that by gun logic at best strtTime = timeAtFiring-1 or less
		long strtTime = lastSeenStatPoint.getTime();

		long tMaxCnt = 100; // maximum calculation depth
		for ( long t = strtTime + 1; t < strtTime+tMaxCnt ; t++) {
			speed = speed + botAcceleration;
			if ( speed >= robocode.Rules.MAX_VELOCITY ) {
				// we reached maximum allowed speed
				speed = robocode.Rules.MAX_VELOCITY;
				botAcceleration = 0;
			}
			if ( speed < 0  ) {
				// we were slowing down and just crossed flip point
				speed = -speed;
				botAcceleration =  robocode.Rules.ACCELERATION;
			}
			phi = phi + botBodyRotationSpeed;
			vxNew = speed * Math.cos(phi);
			vyNew = speed * Math.sin(phi);
			vx = vxNew;
			vy = vyNew;
			posFut.x = posFut.x + vx;
			posFut.y = posFut.y + vy;
			// now we now bot position at time t
			if ( !physics.botReacheableBattleField.contains( posFut ) ) {
				// count one step back when we were on battlefield
				posFut.x = posFut.x - vx;
				posFut.y = posFut.y - vy;
				break;
			}
			if ( (t - timeAtFiring) >= 0 ) {
				// finally we now bot position at timeAtFiring+1
				// at firing time bullet already moved
				if ( firingPosition.distance( posFut) <= (t-timeAtFiring+1)*bSpeed ) {
					// bullet reached the target
					break;
				}
			}
		}
		return posFut;
	}
}

