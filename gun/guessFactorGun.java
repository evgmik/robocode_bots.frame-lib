// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class guessFactorGun extends baseGun {
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

		double latteralSpeed = tBStat.getLateralSpeed( fPos );
		int[] gfBins = fBot.getGunManager().getGuessFactors( tBot.getName() );
		double gf = getMostProbableGF( gfBins ) * math.signNoZero( latteralSpeed );
		double firingAngle = math.angle2pt( fPos, tBStat.getPosition() ); // head on
		firingAngle += gf*physics.calculateMEA( physics.bulletSpeed(bulletEnergy) );
		firingSolution fS = new firingSolution( this, fPos, firingAngle, time, bulletEnergy );
		if (fS == null)
			return fSols;

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
		fSols.add(fS);
		fSols = setFiringBotName( fBot.getName(), fSols );
		fSols = setTargetBotName( tBot.getName(), fSols );
		return fSols;
	}

	private double getMostProbableGF(int[] guessFactorBins ) {
		int numBins = guessFactorBins.length;
		double[] guessFactorWeighted = new double[ numBins ];
		double binsSum = 0;
		int indMax = 0;
		int maxCnt =0;
		for (int i=0; i < numBins; i++ ) {
			binsSum += guessFactorBins[i]; // calculates total count
			if ( guessFactorBins[i] > maxCnt ) {
				maxCnt = guessFactorBins[i];
				indMax = i;
			}
		}
		double gf;
		if ( binsSum == 0 ) {
			// empty statistics
			gf =  0; // head on guess factor
		} else {
			gf =  math.bin2gf( indMax, numBins );
		}
		return gf;
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

		// where target will be at wave intersection according to linear predictor
		Point2D.Double tFPos = misc.linear_predictor( bSpeed, tPos, vTvec,  fPos);

		firingSolution fS = new firingSolution( this, fPos, tFPos, time, bulletEnergy );

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
		// check if the future target point is within botReacheable space
		if ( !physics.botReacheableBattleField.contains( tFPos ) ) {
			logger.noise("time " + time + " unphysical future target positkon");
			fS.setQualityOfSolution( 0 ); // bad solution
		}

		logger.noise("linear gun firingSolution: " + fS.toString());
		fSolultions.add(fS);
		fSolultions = setTargetBotName( tBot.getName(), fSolultions );
		return fSolultions;
	}
}
