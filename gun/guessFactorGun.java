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
	boolean  antiGFavoider = false; // if bot tries to avoid known GF we do reverse weighting

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
		ArrayStats stats = new ArrayStats( guessFactorBins );
		if ( antiGFavoider ) {
			double max = stats.max;
			double[] guessFactorBinsFlipped = new double[ numBins ];
			for (int i=0; i< numBins; i++) {
				// flipping GFs
				guessFactorBinsFlipped[i] = max - guessFactorBins[i];
			}

			double threshold = stats.mean*.5; // spill out weight into a GF bin
			// Essentially GF < threshold were never visited 
			// either we have too little stats  or it is unreachable GF.
			// Unreachable GF should be at edges we set them to zero so
			// they never checked
			// Threshold is quite high and tune heuristically to avoid shooting
			// extreme GFs.
			for (int i=0; i< numBins; i++) {
				// left edge search
				if ( guessFactorBins[i] <= threshold ) {
					guessFactorBinsFlipped[i] = 0;
				} else {
					break;
				}
			}
			for (int i=numBins-1; i>=0; i--) {
				// right edge search
				if ( guessFactorBins[i] <= threshold ) {
					guessFactorBinsFlipped[i] = 0;
				} else {
					break;
				}
			}
			// reassigning everything back to guessFactorBins
			for (int i=0; i< numBins; i++) {
				guessFactorBins[i] = guessFactorBinsFlipped[i];
			}
			stats = new ArrayStats( guessFactorBins );
		}
		if ( !true ) { // enable for dbg
			String sout="";
			sout = logger.arrayToTextPlot( guessFactorBins );
			sout += " for gun " + getName();
			logger.dbg( sout);
		}
		int indMax = stats.indMax;
		double mean = stats.mean;
		double std  = stats.std;
		double binsSum = stats.sum;
		double gf;
		if ( (binsSum == 0) ) {
			// empty statistics
			gf =  0; // head on guess factor
			//gf = Double.NaN;
			return gf;
		}
		//if ( (maxCnt/binsSum) < 2*1.0/numBins) {
		if ( binsSum < binsSumThreshold ) {
			// empty statistics or not strong enough stats
			gf =  0; // head on guess factor
			//gf = Double.NaN;
			return gf;
		} else {
			gf =  math.bin2gf( indMax, numBins );
		}
		//logger.dbg( getName() + " best gf = " + gf);
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


