// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class circularGun extends baseGun {
	public circularGun() {
		gunName = "circularGun";
		color = new Color(0x00, 0x00, 0xff, 0x80);
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		Point2D.Double fPos = fBot.getMotion().getPositionAtTime( time );
		LinkedList<firingSolution> fSols = getFiringSolutions( fPos, tBot, time, bulletEnergy);
		fSols = setFiringBotName( fBot.getName(), fSols );
		return fSols;
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();

		botStatPoint fBStat = fBot.getStatClosestToTime( time );
		if (fBStat == null)
			return fSolultions;
		Point2D.Double fPos = (Point2D.Double) fBStat.getPosition().clone();

		fSolultions = getFiringSolutions( fPos, tBot, time, bulletEnergy);
		fSolultions = setFiringBotName( fBot.getName(), fSolultions );
		return fSolultions;

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

		// position previous to fire time
		botStatPoint bStatPrev = tBot.getStatClosestToTime( time - 2 );

		double phi = 0;
		Point2D.Double posFut  = new Point2D.Double(0,0);
		Point2D.Double vTvecLast, vTvecPrev;

		if ( bStatPrev == null ) {
			phi = 0; // falling back to linear gun
			//return fSolultions; // circular gun is not applicable
		} else {
			vTvecPrev = bStatPrev.getVelocity();
			double phiLast = Math.atan2( vTvec.y, vTvec.x);
			double phiPrev = Math.atan2( vTvecPrev.y, vTvecPrev.x);
			double dt =  tBStat.getTime() - bStatPrev.getTime();
			if ( dt == 0 ) {
				// previous point is the same as current
				phi = 0; // falling back to linear gun
				//return fSolultions; // circular gun is not applicable
			} else {
				phi = (phiLast - phiPrev)/dt;
			}
		}

		double vx = vTvec.x;
		double vy = vTvec.y;

		// rotation coefficients
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);

		double vxNew, vyNew;
		posFut.x = tBStat.getX();
		posFut.y = tBStat.getY();

		int tMaxCnt = 100; // maximum calculation depth
		for ( int t = 1; t < tMaxCnt ; t++) {
			vxNew =  vx * cosPhi - vy * sinPhi;
			vyNew =  vx * sinPhi + vy * cosPhi;
			vx = vxNew;
			vy = vyNew;
			posFut.x = posFut.x + vx;
			posFut.y = posFut.y + vy;
			if ( !physics.botReacheableBattleField.contains( posFut ) ) {
				// count one step back when we were on battlefield
				posFut.x = posFut.x - vx;
				posFut.y = posFut.y - vy;			
				break;
			}
			if ( fPos.distance( posFut) <= t*bSpeed ) {
				// bullet reached the target
				break;
			}
		}
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
}

