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
	boolean aimAtEveryone = true;

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
				bestFiringSolution.setIsRealBulletFlag( true );
				myBot.proxy.setFireBullet(bestFiringSolution.bulletEnergy);
				firedAt.incrHashCounter( targetBot.getName() );
				wave nW = new wave( myBot.getInfoBot(), myBot.getTime(), bestFiringSolution.bulletEnergy );
				// add safety corridors in enemy waves
				for ( waveWithBullets wB: myBot.getEnemyWaves() ) {
					wB.addSafetyCorridor( bestFiringSolution );
				}

				myBot.getGameInfo()._wavesManager.add( nW );
				waveWithBullets wB = new waveWithBullets( nW );
				for ( firingSolution fS: firingSolutions ) {
					wB.addFiringSolution(fS);
				}
				myBot.myWaves.add(wB);
			}
		}
	}

	public void rankAimAtAllSolutions( LinkedList<firingSolution> fSols, double bulletEnergy ) {
		double MEA = physics.calculateMEA( physics.bulletSpeed(bulletEnergy) );
		Point2D.Double myPos = myBot.getPosition();
		double distToGlobalTarget = myPos.distance( targetBot.getPosition() );
		for ( firingSolution fS1 : fSols ) {
			double fS1Q = fS1.getQualityOfSolution();
			double a1 = fS1.getFiringAngle();
			Point2D.Double tPos = myBot.getGameInfo().getFighterBot( fS1.getTargetBotName() ).getPosition();
			double dist = myPos.distance( tPos );
			String tName1 = fS1.getTargetBotName();
			fighterBot tB1 = myBot.getGameInfo().getFighterBot( tName1 );
			double sumWa =  0;
			int cnt =0; 
			for ( firingSolution fS2 : fSols ) {
				if ( fS1 == fS2 || tName1.equals( fS2.getTargetBotName() ) ) {
					continue;
				}
				double a2 = fS2.getFiringAngle();
				double da = Math.abs( math.shortest_arc(a1 - a2) )/MEA;
				//sumWa += Math.exp( - (da*da) );
				sumWa +=  1/(1+(da*da));
				cnt++;
			}
			double angleW = (1 + sumWa)/(1 + cnt); // solution fS1 has weight too
			double distW =  botTargetingWeightByDistance( tB1 );
			double energyW = botTargetingWeightByEnemyEnergy( tB1);
			fS1.setQualityOfSolution( fS1Q * angleW * distW * energyW );
		}
	}

	public LinkedList<firingSolution> getAimAtEveryoneFiringSolutions(double bulletEnergy) {
		LinkedList<firingSolution> fSols    = new LinkedList<firingSolution>();
		LinkedList<firingSolution> fSolsAll = new LinkedList<firingSolution>();
		for ( fighterBot eB : myBot.getEnemyBots() ) {
			//logger.dbg("aiming at everyone");
			// note getTime()+1, the fire command is executed at next tic
			fSols =  getFiringSolutions( eB, myBot.getTime()+1, bulletEnergy );
			fSolsAll.addAll( fSols ); // virtual solutions
		}
		return fSolsAll;
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
			if ( aimAtEveryone ) {
				// firingSolutions are already calculated during bestTargetBot search
				//firingSolutions = getAimAtEveryoneFiringSolutions( bulletEnergy );
				//rankAimAtAllSolutions( firingSolutions, bulletEnergy );
				fSols = firingSolutions;
			} else {
				// aim only at target bot
				// note getTime()+1, the fire command is executed at next tic
				fSols =  getFiringSolutions( targetBot, myBot.getTime()+1, bulletEnergy );
				firingSolutions.addAll( fSols ); // virtual solutions
			}
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
		double bulletEnergy = 0;
		double targetDistance = myBot.getPosition().distance( targetBot.getPosition() );
		//double opt_dist = 300; // chosen somewhat arbitrary
		//double t_max = opt_dist / physics.bulletSpeed( robocode.Rules.MIN_BULLET_POWER );
		//double optBulletSpeed = targetDistance/t_max;
		//bulletEnergy = physics.bulletEnergy( optBulletSpeed );
		//logger.dbg( "dist = " + targetDistance + " bE = " + bulletEnergy );

		//bulletEnergy = 500/targetDistance;
		bulletEnergy = 3*(300*300)/(targetDistance*targetDistance);
		bulletEnergy = Math.min( bulletEnergy, robocode.Rules.MAX_BULLET_POWER);
		// attempting to use the  wiki BasicSurfer x.x5 power detection bug
		// the bug was present in the old wiki version, presumably used 
		// by many bots based on it
		// http://robowiki.net/w/index.php?title=User_talk:Beaming&offset=20151204021729&lqt_mustshow=4826#Fire_power_2.95_bug_4826
		//logger.dbg( "-bE = " + bulletEnergy );
		// no point to fire bullets more energetic than enemy bot energy level
		bulletEnergy = Math.min( bulletEnergy, physics.minReqBulEnergyToKillTarget( targetBot.getEnergy() ) );
		bulletEnergy = (Math.round( bulletEnergy * 10 ) - .5 )/10; // energy = x.x5

		bulletEnergy = Math.max( bulletEnergy, robocode.Rules.MIN_BULLET_POWER );

		return bulletEnergy;
	}

	public void aimAndSetGun( firingSolution fS ) {
		if ( fS == null) {
			logger.noise("time " + myBot.getTime() + " no firing solution. Veto on firing gun " + fS.getGunName() );
			bestFiringSolution = null;
			return;
		}
		if ( fS.getQualityOfSolution() < firingSolutionQualityThreshold ) {
			logger.noise("time " + myBot.getTime() + " bad quality " + fS.getQualityOfSolution() + " of firing solution. Veto on firing gun " + fS.getGunName() );
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
		fighterBot bestTargetBot = targetBot;
		if ( myBot.getEnemyBots().size() == 0 ) {
			return null;
		}
		if ( bestTargetBot == null ) {
			bestTargetBot = findTheBestTargetBotWise();
		} else  if ( aimAtEveryone ) {
			fighterBot bestTargetBotFSWise = findTheBestTargetFSWise();
			if ( bestTargetBot == null || bestTargetBotFSWise == null ) {
				return null;
			}
			if ( !bestTargetBot.getName().equals( bestTargetBotFSWise.getName() ) ) {
				logger.dbg("FS overrides target: " + bestTargetBot.getName() + " ==> " + bestTargetBotFSWise.getName() );
				bestTargetBot = bestTargetBotFSWise;
			}
		}
		reportBestTargetBot( bestTargetBot );
		return bestTargetBot;
	}

	public fighterBot findTheBestTargetFSWise() {
		fighterBot bestTargetBot = null;
		if ( myBot.getEnemyBots().size() == 0 ) {
			return null;
		}
		firingSolutions = new LinkedList<firingSolution>(); //clear the list
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		double bulletEnergy = -1000; // intentionally bad
		firingSolution fS = null;
		if ( targetBot != null ) {
			bulletEnergy = bulletEnergyVsDistance( targetBot );
			if ( bulletEnergy >= (myBot.getEnergy() - 1e-4) ) {
				// do not fire or we will get ourself disabled
				return null;
			}
			bulletEnergy = Math.max( bulletEnergy, 0 ); // zero means no fire
			if ( bulletEnergy <= 0 ) {
				return null; // bad bullet
			}
			firingSolutions = getAimAtEveryoneFiringSolutions( bulletEnergy );
			rankAimAtAllSolutions( firingSolutions, bulletEnergy );
			fSols = firingSolutions;
			fS = getTheBestFiringSolution( fSols ); // real one
		}
		if ( fS == null ) {
			return null;
		}
		bestTargetBot = myBot.getGameInfo().getFighterBot( fS.getTargetBotName() );
		return bestTargetBot;
	}

	public fighterBot findTheBestTargetBotWise() {
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

			//w *= botTargetingWeightByFireAngleSpread(eBot);
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

	public double botTargetingWeightByFireAngleSpread(fighterBot bot) {
		// try to locate a bot which is in the middle of the crowd
		// and crowd within small fire angle spread
		double w=1;
		long time = myBot.getTime() + 1;
		String tName = bot.getName();
		Point2D.Double fP = myBot.getMotion().getPositionAtTime( time );
		Point2D.Double tP =   bot.getMotion().getPositionAtTime( time );
		double angle2target = math.angle2pt(fP, tP); // degrees
		LinkedList<Double> aList  = new LinkedList<Double>();

		int cnt = 0;
		// calculate firing angle relative to the given bot
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			if ( eBot.getName().equals(  tName ) ) {
				continue;
			}
			cnt++;
			tP =   eBot.getMotion().getPositionAtTime( time );
			double a = math.angle2pt(fP, tP) - angle2target;
			a = math.shortest_arc(a);
			aList.add(a);
		}
		if ( cnt <= 2 ) {
			// no point to do spread weight
			return 1;
		}

		// calculate mean
		double aMean = 0;
		for ( double a : aList ) {
			aMean += a;
		}
		aMean = aMean/cnt;

		// calculate variance
		double aVar = 0;
		for ( double a : aList ) {
			aVar += a*a;
		}
		aVar = aVar/cnt;

		double aStd = Math.sqrt( aVar ); // standard deviation

		double aThreshold = 60; 
		double overallWeight = 0.2;
		
		w = Math.exp( - aStd/aThreshold );
		w = ( (1-overallWeight) + overallWeight*w ); // not so large contribution

		//logger.dbg( "bot " + tName + " has neighbors spread = " + aStd + " its weight = " + w );

		return w;
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

	public double botTargetingHitProbWeight(fighterBot bot) {
		// here we estimate hit probability
		double w=1;
		w *= botTargetingWeightByDistance(bot); // random shot hit probability
		int gunStatsReliableRound = 4; // recall that we count from 0
		if ( myBot.getGameInfo().getRoundNum() > gunStatsReliableRound ) {
			//gun stats become reliable only after some time
			double hRate = math.eventRate( hitByMe.getHashCounter( bot.getName() ), firedAt.getHashCounter( bot.getName() ) );
			w = Math.max( w, hRate );
		}
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
		
		w=(2*physics.robotHalfSize)/fullEscapeArc; // ratio of target size to arc
		return w;
	}

	public double botTargetingWeightByScanLag(fighterBot bot) {
		baseGun bG = new baseGun();
		double w;
		w = bG.getLagTimePenalty( myBot.getTime() - bot.getLastSeenTime() ) ;
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
