// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.bot.*;
import eem.frame.misc.*;
import eem.frame.wave.*;
import eem.frame.core.*;

import robocode.Rules.*;
import robocode.BattleRules.*;

import java.util.LinkedList;
import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class masterBotGunManager extends gunManager {
	boolean aimAtEveryone = true;
	boolean useAngleDistribution = false;
	protected HashMap<String, Double> weightsBotWise = new HashMap<String, Double>();
	protected fighterBot mostEnergeticEnemy = null;
	protected double idealBulletEnergy = 1.95;
	protected int bestRoundsStart = 0;
	protected double bestAPS = 0;
	protected int idealBulletEnergyIncrDir = 1;
	protected double bEdiff = 0.5;

	public	masterBotGunManager() {
	}

	public	masterBotGunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public void initBattle() {
		super.initBattle();
		idealBulletEnergy = calcIdealBulletEnergy();
		logger.routine("Probing new bullet energy " + idealBulletEnergy );
	}

	public double calcIdealBulletEnergy() {
		CoreBot cB = myBot.getGameInfo().getMasterBot();
		int roundNum = cB.getRoundNum();
		// nothing fancy here
		return 1.95;
	}

	public void askRadarToTrack() {
		if ( firingSolutions.size() == 0 ) {
			myBot.getRadar().setNeedToTrackTarget( false );
			return;
		}
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
				waveWithBullets wB = new waveWithBullets( nW, myBot.getGunManager().getGuessFactosrBinNum() );
				wB.setTargetBot( targetBot );
				wB.addFiringSolutions( firingSolutions );
				myBot.myWaves.add(wB);
			} else {
				// fire virtual wave
				if ( false || myBot.getTime() < physics.ticTimeFromTurnAndRound(200, 0) ) {
				wave nW = new wave( myBot.getInfoBot(), myBot.getTime(), bestFiringSolution.bulletEnergy );
				myBot.getGameInfo()._wavesManager.add( nW );
				if ( true ) { // DEBUG AND SPEED UP 
				if (firingSolutions.size() > 0 && myBot.getTime() < physics.ticTimeFromTurnAndRound(200, 0) ) {
					// since we have already calculated firing solutions
					// let's add them to a wave
					HashMap<String, waveWithBullets> waveCache = new HashMap<String, waveWithBullets>();
					for (firingSolution fS : firingSolutions ) {
						String targetName = fS.getTargetBotName();
						waveWithBullets wB = waveCache.get(targetName);
						if ( wB == null ) {
							wB = new waveWithBullets( nW, myBot.getGunManager().getGuessFactosrBinNum() );
							wB.setTargetBot( myBot.getGameInfo().getFighterBot( targetName) );
							waveCache.put(targetName, wB);
							myBot.myWaves.add(wB);
						}
						wB.addFiringSolution( fS );
					}

					waveWithBullets wB = new waveWithBullets( nW, myBot.getGunManager().getGuessFactosrBinNum() );
					wB.setTargetBot( targetBot );
					wB.addFiringSolutions( firingSolutions );
					myBot.myWaves.add(wB);
				}
				}
				}
			}
		}
	}

	public void rankSolutionsBasedOnAngleDistribution( fighterBot bestTargetBot, LinkedList<firingSolution> fSols, double bulletEnergy ) {
		double MEA = physics.calculateMEA( physics.bulletSpeed(bulletEnergy) );
		HashMap<firingSolution, Double> weightsPerFS = new HashMap<firingSolution, Double>();
		// calculate additional weight due to angles distribution
		for ( firingSolution fS1 : fSols ) {
			String tName1 = fS1.getTargetBotName();
			double a1 = fS1.getFiringAngle();

			double sumWa = fS1.getQualityOfSolution();
			int cnt =1; 
			for ( firingSolution fS2 : fSols ) {
				if ( fS1 == fS2 || tName1.equals( fS2.getTargetBotName() ) ) {
					continue;
				}
				double a2 = fS2.getFiringAngle();
				double da = Math.abs( math.shortest_arc(a1 - a2) )/MEA;
				//sumWa += Math.exp( - (da*da) );
				sumWa +=  fS2.getQualityOfSolution()*Math.exp(-(da*da));
				cnt++;
			}
			if ( cnt == 0 ) {
				cnt=1;
			}
			double w = fS1.getQualityOfSolution();
			weightsPerFS.put( fS1, w*sumWa/cnt );
		}
		double angleDistrW = 0.2;
		for ( firingSolution fS1 : fSols ) {
			double wDistr = weightsPerFS.get( fS1 );
			double w = fS1.getQualityOfSolution();
			w = (1-angleDistrW)*w + angleDistrW*wDistr;
			fS1.setQualityOfSolution( w );
		}
	}

	public void rankAimAtAllSolutions( fighterBot bestTargetBot, LinkedList<firingSolution> fSols, double bulletEnergy ) {
		//profiler.start( "rankAimAtAllSolutions" );

		//first we rank firing solution based on target bot weights
		if (  myBot.getEnemyBots().size() > 1 ) {
		for ( firingSolution fS1 : fSols ) {
			String tName1 = fS1.getTargetBotName();
			double gunPerf = fS1.getQualityOfSolution();
			double w = 1;
			double wBotWise = 1;
			if ( weightsBotWise.containsKey( tName1 ) ) {
				wBotWise = weightsBotWise.get( tName1 );
			}
			w *= wBotWise;

			int gunStatsReliableRound = 4; // recall that we count from 0
			double perfContr = 0.1;
			if ( myBot.getGameInfo().getRoundNum() > gunStatsReliableRound ) {
				perfContr = 0.1;
			}

			//double gunPerfW = (1-perfContr) + perfContr * gunPerf;
			w =  (1-perfContr)*w + perfContr * gunPerf;
			//w *= gunPerfW;

			fS1.setQualityOfSolution( w );
		}
		}
		if ( useAngleDistribution ) { //disable angle normalization
			rankSolutionsBasedOnAngleDistribution( bestTargetBot, fSols, bulletEnergy );
		}
		//profiler.stop( "rankAimAtAllSolutions" );
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
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		double bulletEnergy = -1000; // intentionally bad
		if ( targetBot != null ) {
			bulletEnergy = bulletEnergyVsDistance( targetBot );
			bulletEnergy = Math.max( bulletEnergy, 0 ); // zero means no fire
			if ( bulletEnergy <= 0 ) {
				return; // bad bullet
			}
			if ( aimAtEveryone ) {
				// firingSolutions are already calculated during bestTargetBot search
				fSols = firingSolutions;
			} else {
				// aim only at target bot
				// note getTime()+1, the fire command is executed at next tic
				firingSolutions = new LinkedList<firingSolution>(); //clear the list
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
			if ( !aimAtEveryone ) { // add firing solutions for other bots
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
	}

	public void manage() {
		profiler.start( "gunManagerManage" );
		askRadarToTrack();
		fireTheGunIfShould();
		profiler.start( "gunManagerManage.findTheBestTarget" );
		targetBot = findTheBestTarget();
		profiler.stop( "gunManagerManage.findTheBestTarget" );
		profiler.start( "gunManagerManage.aimTheGun" );
		aimTheGun();
		profiler.stop( "gunManagerManage.aimTheGun" );
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
		if ( myBot.getGameInfo().fightType().equals("1on1") ) { // below 3 lines were noticed in cs.Nene logic
			// strangely fixed bulletEnergy = 1.95 helps a lot against strong bots
			bulletEnergy = idealBulletEnergy;
			if(targetDistance < 140)
				bulletEnergy = 2.95;
		} else {
			bulletEnergy = (idealBulletEnergy/1.95)*3*(300*300)/(targetDistance*targetDistance);
		}
		if ( mostEnergeticEnemy != null ) {
			if ( myBot.getEnergy() > (mostEnergeticEnemy.getEnergy()+20) ) {
				// we have some energy to spare
				bulletEnergy *=2;
			}
		}
		bulletEnergy = Math.min( bulletEnergy, robocode.Rules.MAX_BULLET_POWER);
		// attempting to use the  wiki BasicSurfer x.x5 power detection bug
		// the bug was present in the old wiki version, presumably used 
		// by many bots based on it
		// http://robowiki.net/w/index.php?title=User_talk:Beaming&offset=20151204021729&lqt_mustshow=4826#Fire_power_2.95_bug_4826
		//logger.dbg( "-bE = " + bulletEnergy );
		// no point to fire bullets more energetic than enemy bot energy level
		bulletEnergy = Math.min( bulletEnergy, physics.minReqBulEnergyToKillTarget( targetBot.getEnergy() ) );
		bulletEnergy = Math.min( bulletEnergy, (myBot.getEnergy() - 1e-3) );
		bulletEnergy = (Math.round( bulletEnergy * 10 ) - .5 )/10; // energy = x.x5

		bulletEnergy = Math.max( bulletEnergy, robocode.Rules.MIN_BULLET_POWER );
		double energySurplus = myBot.getEnergy() - mostEnergeticEnemy.getEnergy();
		if ( true ) { // looks like below is bad idea 1on1 performance drops a lot
		if ( myBot.getEnemyBots().size() == 1 && (myBot.getEnergy()<30) && energySurplus < 15 ) {
			// if we below enemy energy we need to shoot to survive 
			// since smart one will stop firing and we are screwed
			// but we need to fire lower energy in a hope to get even
			double enemyBullet = mostEnergeticEnemy.getGunManager().getLastFiredBullet();
			double myEnergyDrain = 0;
			double enemyEnergyDrain = 0;

			double diffDrain = 0;
			double myTimeToDie = Double.POSITIVE_INFINITY;
			double enemyTimeToDie = Double.POSITIVE_INFINITY;

			double firstEstimateBulletEnergy = bulletEnergy;
			bulletEnergy *=2;
			int cnt=0;
			do {
				cnt++;
				bulletEnergy /= 2;
				if ( energySurplus <0 ) {
					bulletEnergy = robocode.Rules.MIN_BULLET_POWER;
				}
				bulletEnergy = Math.max( bulletEnergy, robocode.Rules.MIN_BULLET_POWER );
				myEnergyDrain = energyDrainPerTickByEnemy(enemyBullet, mostEnergeticEnemy) - energyGainPerTickFromEnemy( bulletEnergy, mostEnergeticEnemy);
				enemyEnergyDrain = mostEnergeticEnemy.getGunManager().energyDrainPerTickByEnemy(bulletEnergy, myBot) - mostEnergeticEnemy.getGunManager().energyGainPerTickFromEnemy( enemyBullet, myBot);
				diffDrain = myEnergyDrain - enemyEnergyDrain;
				myTimeToDie = myBot.getEnergy()/myEnergyDrain;
				enemyTimeToDie = mostEnergeticEnemy.getEnergy()/enemyEnergyDrain;
				if ( energySurplus >0 && diffDrain < 0) {
					break; // nothing to worry
				}
				if ( energySurplus >0 && diffDrain > 0) {
					if ( ( energySurplus / Math.max(diffDrain,0.01) ) > Math.max(2*enemyTimeToDie, 200) ) {
						// can we maintain energySurplus long enough
						break;
					}

				}
				if (myTimeToDie > enemyTimeToDie - .2*Math.max(myTimeToDie,enemyTimeToDie)) {
					// we will outlive the enemy
					break;
				}
				if (cnt>10) {
					logger.error("Error: something horribly wrong with bulletEnergy calculation");
					bulletEnergy = robocode.Rules.MIN_BULLET_POWER;
					break;
				}
				//logger.dbg("my energy drain = " + myEnergyDrain + " enemy energy drain = " + enemyEnergyDrain );
				//logger.dbg("myEnergy = " + myBot.getEnergy() + " myTimeToDie = " + myTimeToDie + " enemyTimeToDie = " + enemyTimeToDie );
				//logger.dbg("tic " + myBot.getTime() + ": under cutting bullet energy = " + bulletEnergy);
			} while ( bulletEnergy > robocode.Rules.MIN_BULLET_POWER );
			if ( (energySurplus < -1 ) && (myTimeToDie > 0) && (enemyTimeToDie > 0 ) && (bulletEnergy == robocode.Rules.MIN_BULLET_POWER) &&(myTimeToDie < enemyTimeToDie-100 ) ) {
				// looks like we screwed, enemy will outlive us
				// let's drain our energy as quickly as possible
				// so the enemy will not get damage points
				//logger.dbg("tic " + myBot.getTime() + " looks like we are going to lose, betting on high energy bullets");
				//logger.dbg("my energy drain = " + myEnergyDrain + " enemy energy drain = " + enemyEnergyDrain );
				//logger.dbg("myEnergy = " + myBot.getEnergy() + " my bullet energy = " + bulletEnergy + " enemyBullet = " + enemyBullet + " myTimeToDie = " + myTimeToDie + " enemyTimeToDie = " + enemyTimeToDie );
				bulletEnergy = Math.min(firstEstimateBulletEnergy, myBot.getEnergy()-.01);
				//logger.dbg("setting bullet to " + bulletEnergy );
			}
		}
		}
		long numEnemyWaveAtLeast = 0;
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			numEnemyWaveAtLeast += myBot.getGameInfo().getWavesManager().getWavesOfBot(eBot).size();
			if ( numEnemyWaveAtLeast > 0 ) { break; };
		}
		// now if enemy is not firing and I am more energetic let's try to stay
		// that way, otherwise if we miss we go below and it is no good
		// if my hit probability low, we might not recover
		if ( 
				//numEnemyWaveAtLeast <= 1 // enemies stopped or about to stop firing
				( myBot.getEnemyBots().size() == 1 || numEnemyWaveAtLeast == 0 )
				&& ( (myBot.getTime() - physics.getRoundStartTime(myBot.getTime())) > 35 ) 
				&& energySurplus > 0
		) {
			// enemy is not firing
			energySurplus = myBot.getEnergy() - 5*robocode.Rules.MIN_BULLET_POWER - mostEnergeticEnemy.getEnergy();
			if ( energySurplus < 0 ) {
				// stop firing to maintain energy surplus
				bulletEnergy = -1;
			} else {
				bulletEnergy = Math.min( bulletEnergy, energySurplus );
				bulletEnergy = Math.max( bulletEnergy, robocode.Rules.MIN_BULLET_POWER );
			}
		}
		if ( (myBot.getEnergy() - bulletEnergy) < (1.5*physics.depletedEnergyLevel) ) {
			// do not fire or we will get yourself disabled
			bulletEnergy = -1; // negative = no fire
		}

		//logger.dbg("calculated bulletEnergy = " + bulletEnergy);
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
		bestTargetBot = findTheBestTargetBotWise();
		//reportBestTargetBot( bestTargetBot );
		if ( aimAtEveryone ) {
			fighterBot bestTargetBotFSWise = findTheBestTargetFSWise( bestTargetBot );
			if ( bestTargetBot == null || bestTargetBotFSWise == null ) {
				return null;
			}
			if ( !bestTargetBot.getName().equals( bestTargetBotFSWise.getName() ) ) {
				//logger.dbg("FS overrides target: " + bestTargetBot.getName() + " ==> " + bestTargetBotFSWise.getName() );
				bestTargetBot = bestTargetBotFSWise;
			}
		}
		reportBestTargetBot( bestTargetBot );
		return bestTargetBot;
	}

	public fighterBot findTheBestTargetFSWise( fighterBot bestTargetBot ) {
		if ( myBot.getEnemyBots().size() == 0 ) {
			return bestTargetBot;
		}
		firingSolutions = new LinkedList<firingSolution>(); //clear the list
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		double bulletEnergy = -1000; // intentionally bad
		firingSolution fS = null;
		if ( bestTargetBot != null ) {
			bulletEnergy = bulletEnergyVsDistance( bestTargetBot );
			bulletEnergy = Math.max( bulletEnergy, 0 ); // zero means no fire
			if ( bulletEnergy <= 0 ) {
				return bestTargetBot; // bad bullet
			}
			firingSolutions = getAimAtEveryoneFiringSolutions( bulletEnergy );
			rankAimAtAllSolutions( bestTargetBot,  firingSolutions, bulletEnergy );
			fSols = firingSolutions;
			fS = getTheBestFiringSolution( fSols ); // real one
		}
		if ( fS == null ) {
			return bestTargetBot;
		}
		bestTargetBot = myBot.getGameInfo().getFighterBot( fS.getTargetBotName() );
		return bestTargetBot;
	}

	public fighterBot findTheBestTargetBotWise() {
		fighterBot bestTargetBot = null;
		if ( myBot.getEnemyBots().size() == 0 ) {
			return null;
		}
		mostEnergeticEnemy = null;
		double bestWeight = -1e6;
		double w=1;
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			// updating most energetic enemy, we will need it later
			if ( mostEnergeticEnemy == null ) {
				mostEnergeticEnemy = eBot;
			} else if ( eBot.getEnergy() > mostEnergeticEnemy.getEnergy() ) {
				mostEnergeticEnemy = eBot;
			}
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
				//w *= botTargetingWeightByFiredShots(eBot);
				w *= botTargetingWeightByHitRate(eBot);
			}

			weightsBotWise.put( eBot.getName(), w);

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

	public void  drawFiringSolutions( Graphics2D g ) {
		double R0 = 150;

		//find max Q
		double Qmax = Double.NEGATIVE_INFINITY;
		double bestA = 0;
		Color bestFScolor = null;

		for ( firingSolution f: firingSolutions ) {
			double q = f.getQualityOfSolution();
			if ( q > Qmax ) {
				Qmax = q;
				bestA = f.getFiringAngle();
				bestFScolor = f.getColor();
			}
		}
		if ( Qmax == 0 ) {
			Qmax = 1;
		}

		Point2D.Double fP = null;
		// now we draw firing angles directions
		for ( firingSolution f: firingSolutions ) {
			double a = math.game_angles2cortesian( f.getFiringAngle() );
			fP = f.getFiringPositon();
			double R = R0*f.getQualityOfSolution()/Qmax;
			double dx = R*Math.cos( Math.toRadians(a) );
			double dy = R*Math.sin( Math.toRadians(a) );
			Point2D.Double endP = new Point2D.Double( fP.x + dx, fP.y + dy );
			g.setColor( f.getColor() );
			graphics.drawLine( g, fP,  endP );

		}
		if ( firingSolutions.size() != 0 ) {
			//draw a circle with max Q size
			g.setColor( bestFScolor );
			graphics.drawCircle( g, fP,  R0 );
			double markerOffset =5;
			double markerR = 4;
			double R = R0 + markerOffset;
			double dx = R*Math.sin( Math.toRadians(bestA) );
			double dy = R*Math.cos( Math.toRadians(bestA) );
			Point2D.Double endP = new Point2D.Double( fP.x + dx, fP.y + dy );
			graphics.drawCircle( g, endP,  markerR );
		}
	}

	public void  drawGunsHitProb( Graphics2D g ) {
		double dy = 8;
		double xOffs = 4;
		double statW0 = 50;
		double statH = dy -2;
		for ( fighterBot eB : myBot.getEnemyBots() ) {
			long cnt = 0;
			Point2D.Double eBpos = eB.getPosition();
			double cornX = eBpos.x + physics.robotHalfSize + xOffs;
			double cornY = eBpos.y - physics.robotHalfSize + statH/2;
			for ( baseGun gun : gunList ) {
				String2D key = new String2D( gun.getName(), eB.getName() );
				double gunPerfRate = math.perfRate( hitByMyGun.getHashCounter(key) , firedAtEnemyByGun.getHashCounter(key) );
				Point2D.Double gStatPos = new Point2D.Double( cornX, cornY );
				double statW = statW0*gunPerfRate;
				gStatPos.x += statW/2;
				gStatPos.y += dy*cnt;

				g.setColor( gun.getColor() );
				graphics.fillRect( g, gStatPos,  statW, statH );


				cnt++;
			}
		}
	}

	public void onPaint(Graphics2D g) {
		super.onPaint( g );
		drawFiringSolutions( g );
		drawGunsHitProb( g );
	}
}
