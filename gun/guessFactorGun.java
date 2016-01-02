// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class guessFactorGun extends baseGun {
	double[] treePointCoord;

	protected double binsSumThreshold = 30; // some heuristic to estimate gun quality
	public guessFactorGun() {
		gunName = "guessFactorGun";
		color = new Color(0xff, 0x88, 0xff, 0x80);
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		Point2D.Double fPos = fBot.getMotion().getPositionAtTime( time );

		// the latest time, when target stats are known, is at 'time-1'
		botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
		if (tBStat == null)
			return fSols;

		setTreePointCoord( calcTreePointCoord( fBot, tBot, time, bulletEnergy ) );

		double latteralSpeed = tBStat.getLateralSpeed( fPos );
		double[] gfBins = getRelevantGF( fBot, tBot );
		double gf = getMostProbableGF( gfBins ) * math.signNoZero( latteralSpeed );
		if ( Double.isNaN( gf ) ) {
			// no enough stats
			return fSols;
		}
		double firingAngle = math.angle2pt( fPos, tBStat.getPosition() ); // head on
		firingAngle += gf*physics.calculateMEA( physics.bulletSpeed(bulletEnergy) );
		firingSolution fS = new firingSolution( this, fPos, firingAngle, time, bulletEnergy );
		setDistanceAtLastAimFor( fS, fPos, tBStat.getPosition() );
		if (fS == null)
			return fSols;

		long infoLagTime = time - tBStat.getTime(); // ideally should be 0
		fS.setQualityOfSolution( getLagTimePenalty( infoLagTime ) );
		fS = correctForInWallFire(fS);
		fSols.add(fS);
		fSols = setFiringBotName( fBot.getName(), fSols );
		fSols = setTargetBotName( tBot.getName(), fSols );
		return fSols;
	}

	protected double[] getRelevantGF( fighterBot fBot, InfoBot tBot ) {
		return  fBot.getGunManager().getGuessFactors( tBot.getName() );
	}

	protected double getMostProbableGF(double[] guessFactorBins ) {
		int numBins = guessFactorBins.length;
		double[] guessFactorWeighted = new double[ numBins ];
		double b = 0;
		double binsSum = 0;
		double binsSqSum = 0;
		int indMax = 0;
		int indMin = 0;
		int nonZeroBinsN = 0;
		double maxCnt =  Double.NEGATIVE_INFINITY;
		double minCnt =  Double.POSITIVE_INFINITY;
		for (int i=0; i < numBins; i++ ) {
			b = guessFactorBins[i];
			binsSum += b; // calculates total count
			binsSqSum += b*b;
			if ( b > maxCnt ) {
				maxCnt = b;
				indMax = i;
			}
			if ( b < minCnt ) {
				minCnt = b;
				indMin = i;
			}
			if ( b != 0 ) {
				nonZeroBinsN++;
			}
		}
		double mean = binsSum/numBins;
		double std  = Math.sqrt( (binsSqSum - mean*mean)/ numBins );
		double gf;
		if ( (binsSum == 0) ) {
			// empty statistics
			gf =  0; // head on guess factor
			gf = Double.NaN;
			return gf;
		}
		//if ( (maxCnt/binsSum) < 2*1.0/numBins) {
		if ( binsSum < binsSumThreshold ) {
			// empty statistics or not strong enough stats
			gf =  0; // head on guess factor
			gf = Double.NaN;
			return gf;
		} else {
			gf =  math.bin2gf( indMax, numBins );
		}
		return gf;
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		logger.error("getFiringSolutions should not be called like this: ( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy )");
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();
		return fSolultions;
	}

	public LinkedList<firingSolution> getFiringSolutions( Point2D.Double fPos, InfoBot tBot, long time, double bulletEnergy ) {
		logger.error("getFiringSolutions should not be called like this: ( Point2D.Double fPos, InfoBot tBot, long time, double bulletEnergy )");
		LinkedList<firingSolution> fSolultions = new LinkedList<firingSolution>();
		return fSolultions;
	}

	// to be overwritten by child guns
	protected double[] calcTreePointCoord( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		double[] coord = null;
		return null;
	}

	public double[] getTreePointCoord() {
		return treePointCoord;
	}

	public void setTreePointCoord(double[] coord) {
		treePointCoord = coord;
	}
}


