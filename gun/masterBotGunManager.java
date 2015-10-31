// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.bot.*;
import eem.frame.misc.*;
import eem.frame.wave.*;

import robocode.Rules.*;
import robocode.BattleRules.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class masterBotGunManager extends gunManager {

	public	masterBotGunManager() {
	}

	public	masterBotGunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public void askRadarToTrack() {
		if ( (myBot.proxy.getGunHeat()/physics.gunCoolingRate - 1 ) < (180/robocode.Rules.RADAR_TURN_RATE) ) {
			myBot.getRadar().setNeedToTrackTarget( true );
		} else {
			myBot.getRadar().setNeedToTrackTarget( false );
		}
	}

	public void fireTheGunIfShould() {
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
				}
				myBot.myWaves.add(wB);
			}
		}
	}

	public void aimTheGun() {
		if ( myBot.proxy.getGunHeat()/physics.gunCoolingRate >  (180/robocode.Rules.GUN_TURN_RATE + 1) ) {
			// do not waste CPU on aiming hot gun
			return;
		}
		firingSolutions = new LinkedList<firingSolution>(); //clear the list
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
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
			// note getTime()+1, the fire command is executed at next tic
			fSols =  getFiringSolutions( targetBot, myBot.getTime()+1, bulletEnergy );
			firingSolutions.addAll( fSols ); // virtual solutions
			firingSolution fS = getTheBestFiringSolution( fSols ); // real one
			if ( fS == null) {
				logger.noise("time " + myBot.getTime() + " Veto on fire: no firing solution");
				return; // no solution
			}
			if ( isBulletShieldDetected( targetBot.getName() ) ) {
				//logger.dbg( "time " + myBot.getTime() +" bullet shield detected for " + targetBot.getName() );
				// apply small angle shift of couple degrees
				// to prevent bullet shield to work
				double offsetAngleAmp = 1; // ~ 180*pi*atan(18/1000)
				double offsetAngle = offsetAngleAmp* math.signNoZero(Math.random()-0.5);
				fS.offsetFiringAngle( offsetAngle );
			}
			aimAndSetGun( fS );
			// if getNumEnemyAlive is too large we have a lot of skipped turns
			if ( myBot.getGameInfo().getNumEnemyAlive() <= 4 ) {
				// now we add virtual solutions for other bots
				for ( fighterBot eBot: myBot.getEnemyBots() ) {
					// skip targetBot we already have its firing solutions
					if ( !eBot.getName().equals( targetBot.getName() ) ) {
						// FIXME: be smart about game stage, bot distance ...
						// note getTime()+1, the fire command is executed at next tic
						fSols =  getFiringSolutions( eBot, myBot.getTime()+1,  bulletEnergy );
						firingSolutions.addAll( fSols ); // virtual solutions
					}
				}
			}
		}
	}

	public void manage() {
		profiler.start( "gunManagerManage" );
		askRadarToTrack();
		fireTheGunIfShould();
		targetBot = findTheBestTarget();
		aimTheGun();
		profiler.stop( "gunManagerManage" );
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
			logger.noise("time " + myBot.getTime() + " bad firing solution. Veto on firing gun " + fS.getGunName() );
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
		fighterBot bestTargetBot = null;
		if ( myBot.getEnemyBots().size() == 0 ) {
			return null;
		}
		double bestWeight = -1e6;
		double w=1;
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			w = 1;
			w *= botTargetingWeightByDistance(eBot);
			w *= botTargetingWeightByScanLag(eBot);
			if ( w >=1 ) {
				// do not even think this bot is so close
				// that we will hit it for sure
				// its info is up-to-date as well
				bestTargetBot = eBot;
				break;
			}
			w *= botTargetingWeightByEnemyEnergy(eBot);

			int gunStatsReliableRound = 4; // recall that we count from 0
			if ( myBot.getGameInfo().getRoundNum() > gunStatsReliableRound ) {
				//gun stats become reliable only after some time
				w *= botTargetingWeightByFiredShots(eBot);
				w *= botTargetingWeightByHitRate(eBot);
			}

			if (w > bestWeight ) {
				bestWeight = w;
				bestTargetBot = eBot;
			}
		}

		reportBestTargetBot( bestTargetBot );
		return bestTargetBot;
	}

	public void reportBestTargetBot( fighterBot bestTargetBot ) {
		// helper to report bestTargetBot
		if ( bestTargetBot != null ) {
			if ( targetBot != null ) {
				if ( !targetBot.getName().equals( bestTargetBot.getName() ) ) {
					logger.routine( "" + myBot.getTime() + " best target " + bestTargetBot.getName() );
				}
			} else {
				logger.routine( "" + myBot.getTime() + " best target " + bestTargetBot.getName() );
			}
		}
	}

	public double botTargetingWeightByHitRate(fighterBot bot) {
		// prey on week and also hope they die first
		double w=1;
		w = math.eventRate( hitByMe.getHashCounter( bot.getName() ), firedAt.getHashCounter( bot.getName() ) );
		double overallWeight = 0.2; // if its large we will fire to far away targets
		w = ( (1-overallWeight) + overallWeight*w ); // not so large contribution
		return w;
	}

	public double botTargetingWeightByEnemyEnergy(fighterBot bot) {
		// prey on week and also hope they die first
		double w=1;
		double energy = bot.getEnergy();
		w = 1 - Math.tanh( energy/50 ); // peak around 30
		double overallWeight = 0.2; 
		w = ( (1-overallWeight) + overallWeight*w );
		return w;
	}

	public double botTargetingWeightByFiredShots(fighterBot bot) {
		// see how many shots I fired at this bot
		// long lived bot are tough to hit so they should be dealt with
		// when easy are gone
		double w=1;
		double fCnt = firedAt.getHashCounter( bot.getName() );
		w = Math.exp( - fCnt / 40 );
		return w;
	}

	public double botTargetingWeightByDistance(fighterBot bot) {
		double w = 1;
		double dist = myBot.getPosition().distance( bot.getPosition() );
		// at certain distances hit probability is 1
		// this is when escape angle is smaller than bot body angle,
		// then random hit probability drops accordingly
		double slowestBulletSpeed = robocode.Rules.getBulletSpeed( robocode.Rules.MAX_BULLET_POWER ); // 11
		double travelTime = dist/slowestBulletSpeed;
		double fullEscapeArc = 2*robocode.Rules.MAX_VELOCITY * travelTime;
		
		w=(2*physics.robotHalfSize)/fullEscapeArc; // ratio of targer size to arc
		return w;
	}

	public double botTargetingWeightByScanLag(fighterBot bot) {
		long infoDelayTimeThreshold = (long) (360/robocode.Rules.RADAR_TURN_RATE + 1);
		double w;
		w = Math.exp ( -(myBot.getTime() - bot.getLastSeenTime()) / (3*infoDelayTimeThreshold) );
		return w;
	}

	public fighterBot findTheClosestTargetWithSwitchTrechold() {
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
