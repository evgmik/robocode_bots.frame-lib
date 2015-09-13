// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.bot.*;
import eem.frame.misc.*;
import eem.frame.wave.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class masterBotGunManager extends gunManager {

	public	masterBotGunManager() {
		gunList = new LinkedList<baseGun>();
		gunList.add( new linearGun() );
		gunList.add( new headOnGun() );
	}

	public	masterBotGunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public void manage() {
		if ( myBot.getTime() == fireAtTime &&  myBot.proxy.getGunTurnRemaining() == 0) {
			// see firing pitfalls
			// http://robowiki.net/wiki/Robocode/Game_Physics#Firing_Pitfall
			// essentially we need to set firing solution at previous tick
			if ( myBot.proxy.getGunHeat() == 0 ) {
				myBot.proxy.setFireBullet(bestFiringSolution.bulletEnergy);
				firedAt.incrHashCounter( targetBot.getName() );
				wave nW = new wave( myBot.getInfoBot(), myBot.getTime(), bestFiringSolution.bulletEnergy );
				myBot.getGameInfo()._wavesManager.add( nW );
				waveWithBullets wB = new waveWithBullets( nW );
				for ( firingSolution fS: firingSolutions ) {
					wB.addFiringSolution(fS);
					incrFiredAtEnemyByGun( fS );
				}
				myBot.myWaves.add(wB);
			}
		}
		firingSolutions = new LinkedList<firingSolution>(); //clear the list
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		targetBot = findTheBestTarget();
		double bulletEnergy = -1000; // intentionally bad
		if ( targetBot != null ) {
			bulletEnergy = bulletEnergyVsDistance( targetBot );
			if ( bulletEnergy >= (myBot.getEnergy() - 1e-4) ) {
				// do not fire or we will get ourself disabled
				return;
			}
			bulletEnergy = Math.max( bulletEnergy, 0 ); // zero means no fire
			if ( bulletEnergy <= 0 ) {
				return; // bad bullet
			}
			fSols =  getFiringSolutions( targetBot, bulletEnergy );
			firingSolutions.addAll( fSols ); // virtual solutions
			firingSolution fS = getTheBestFiringSolution( fSols ); // real one
			if ( fS == null) {
				logger.noise("time " + myBot.getTime() + " Veto on fire: no firing solution");
				return; // no solution
			}
			aimAndSetGun( fS );
			// now we add virtual solutions for other bots
			for ( fighterBot eBot: myBot.getEnemyBots() ) {
				// skip targetBot we already have its firing solutions
				if ( !eBot.getName().equals( targetBot.getName() ) ) {
					// FIXME: be smart about game stage, bot distance ...
					fSols =  getFiringSolutions( eBot, bulletEnergy );
					firingSolutions.addAll( fSols ); // virtual solutions
				}
			}
		}
	}

	protected double bulletEnergyVsDistance( fighterBot targetBot ) {
		double targetDistance = myBot.getPosition().distance( targetBot.getPosition() );
		double bulletEnergy = Math.min( 500/targetDistance, robocode.Rules.MAX_BULLET_POWER);
		// no point to fire bullets more energetic than enemy bot energy level
		bulletEnergy = Math.min( bulletEnergy, physics.minReqBulEnergyToKillTarget( targetBot.getEnergy() ) );

		bulletEnergy = Math.max( bulletEnergy, robocode.Rules.MIN_BULLET_POWER );

		return bulletEnergy;
	}

	public void aimAndSetGun( firingSolution fS ) {
		if ( fS == null) {
			bestFiringSolution = null;
			return;
		}
		if ( fS.getQualityOfSolution() < firingSolutionQualityThreshold ) {
			logger.noise("time " + myBot.getTime() + " Veto on fire: no good enough solution");
			return; // no good enough solution
		}
		logger.noise("time " + myBot.getTime() + " firing solution is good");

		double bulletEnergy = fS.bulletEnergy;
		double firingAngle = fS.firingAngle;
		double gunAngle = myBot.proxy.getGunHeading();
		double angle = math.shortest_arc(firingAngle-gunAngle);
		myBot.proxy.setTurnGunRight(angle);
		fireAtTime = myBot.getTime() + 1;
		bestFiringSolution = fS;
		// now we need to be smart robocode engine first fires than rotate
		// the gun see
		// http://robowiki.net/wiki/Robocode/Game_Physics#Firing_Pitfall
		// so we need to fire only if required gun rotation smaller than
		// target arc.
		// So we do not fire now, instead we will check at next click
		// that gun did rotate enough
	}

	public fighterBot findTheBestTarget() {
		double distThreshold=100; // we keep old target if candidate is not closer than this
		// above should help with continues gun shift
		// when to bots are at about same distance

		long infoDelayTimeThreshold = (long) (360/robocode.Rules.RADAR_TURN_RATE + 1);
		if ( myBot.getEnemyBots().size() == 0 ) {
			return null;
		}
		// very simple algorithm: chose the nearest bot
		double dist2closestBot = 1e6; // something very large
		if  (targetBot != null) {
		       if ( (myBot.getTime() - targetBot.getLastSeenTime()) < infoDelayTimeThreshold ) {
			// let's keep old target bot as reference point
			dist2closestBot = myBot.getPosition().distance( targetBot.getPosition() );
		       }
		}
		double distNew;
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			distNew = myBot.getPosition().distance( eBot.getPosition() );
			if ( ((distNew + distThreshold) < dist2closestBot) && ((myBot.getTime() - eBot.getLastSeenTime()) < infoDelayTimeThreshold) ) {
				dist2closestBot = distNew;
				targetBot = eBot;
			}
		}
		return targetBot;
	}
	
	public LinkedList<firingSolution> getFiringSolutions( fighterBot targetBot, double bulletEnergy ) {
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		// generate solutions for each gun
		for ( baseGun g : gunList ) {
			// note getTime()+1, the fire command is executed at next tic
			LinkedList<firingSolution> gunfSols =  g.getFiringSolutions( myBot, targetBot.getInfoBot(), myBot.getTime()+1, bulletEnergy );
			String2D key = new String2D( g.getName(), targetBot.getName() );
			double gunPerfRate = math.perfRate( hitByMyGun.getHashCounter(key) , firedAtEnemyByGun.getHashCounter(key) );
			for ( firingSolution fS: gunfSols ) {
				double solQ = fS.getQualityOfSolution();
				fS.setQualityOfSolution( solQ * gunPerfRate );
				
			}
			fSols.addAll( gunfSols );
		}
		return fSols;
	}

	public firingSolution getTheBestFiringSolution( LinkedList<firingSolution> fSols ) {
		firingSolution fS = null;
		double bestQSol = -1000;
		for ( firingSolution curFS : fSols ) {
			if ( curFS.getQualityOfSolution() > bestQSol ) {
				fS = curFS;
				bestQSol = curFS.getQualityOfSolution();
			}
		}
		return fS;
	}
}
