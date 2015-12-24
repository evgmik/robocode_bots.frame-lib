// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.util.Arrays;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class assistedGFGun extends guessFactorGun {
	public assistedGFGun() {
		gunName = "assistedGFGun";
		color = new Color(0xaa, 0x88, 0xff, 0x80);
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		Point2D.Double fPos = fBot.getMotion().getPositionAtTime( time );

		// the latest time, when target stats are known, is at 'time-1'
		botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
		if (tBStat == null)
			return fSols;

		double latteralSpeed = tBStat.getLateralSpeed( fPos );
		double[] gfBins = getRelevantGF( fBot, tBot, time, bulletEnergy);
		double gf = getMostProbableGF( gfBins ) * math.signNoZero( latteralSpeed );
		double firingAngle = math.angle2pt( fPos, tBStat.getPosition() ); // head on
		firingAngle += gf*physics.calculateMEA( physics.bulletSpeed(bulletEnergy) );
		if ( Double.isNaN( gf ) ) {
			// no enough stats
			return fSols;
		}
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

	protected double[] getRelevantGF( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		double[][] assistedGFBins = fBot.getGunManager().getAssistedGuess( tBot.getName() ); 
		int numGuessFactorBins = assistedGFBins[0].length;
		double circGF = 0;
		circularGun circGun = new circularAccelGun();
		LinkedList<firingSolution> fSs = circGun.getFiringSolutions( fBot, tBot, time, bulletEnergy );
		if ( fSs.size() >= 1 ) {
			double cAngle = fSs.getFirst().getFiringAngle();
			Point2D.Double fPos = fBot.getMotion().getPositionAtTime( time );
			botStatPoint tBStat = tBot.getStatClosestToTime( time - 1 );
			double headOnAngle = math.angle2pt( fPos, tBStat.getPosition() ); // head on
			cAngle -= headOnAngle;
			cAngle = math.shortest_arc(cAngle);
			circGF = cAngle/physics.calculateMEA( physics.bulletSpeed(bulletEnergy) );
		}
		int j = (int)math.gf2bin( circGF, numGuessFactorBins );
		j = (int)math.putWithinRange( j, 0, (numGuessFactorBins-1) );

		double gfBins[] = assistedGFBins[j];
		return  gfBins;
	}

}

