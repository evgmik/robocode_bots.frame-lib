// -*- java -*-

package eem.frame.bot;

import eem.frame.core.*;
import eem.config.*;
import eem.frame.event.*;
import eem.frame.bot.*;
import eem.frame.radar.*;
import eem.frame.wave.*;
import eem.frame.gameInfo.*;
import eem.frame.motion.*;
import eem.frame.gun.*;
import eem.frame.misc.*;

import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;
import java.lang.Integer;

import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

/*
 * This is main simulator class which should handle situation as it perceived by a bot.
 * Use it for both enemy and our own, but set motion, radar, ... methods appropriately.
 * */

// IMPORTANT
// fighterBotConfig must implement
// public void setDrivers( fighterBot bot,  gameInfo gInfo, boolean isItMasterBotDriver 
// which sets proxy, _radar, _motion, _gunManager 
// for masterBot and enemyBot

public class fighterBot extends fighterBotConfig implements waveListener, botListener {
	protected InfoBot fBot;
	protected gameInfo _gameinfo;
	protected long latestWaveHitTime=0;
	protected double scheduledEdrop = 0;
	protected int  firedCount = 0;
	protected int  hitCount = 0;

	public double myScore = 0, myScoreTotal = 0;
	public double enemyScore = 0, enemyScoreTotal = 0;;

	public LinkedList<waveWithBullets> enemyWaves = new LinkedList<waveWithBullets>();
	public LinkedList<waveWithBullets> myWaves    = new LinkedList<waveWithBullets>();

	public HashMap<String,fighterBot> enemyBots = new HashMap<String, fighterBot>();
	public HashMap<String,fighterBot> allKnownEnemyBots = new HashMap<String, fighterBot>();

	public fighterBot( InfoBot fBot, gameInfo gInfo) {
		this.fBot = fBot;
		_gameinfo = gInfo;
		_gameinfo._wavesManager.addWaveListener( this );
		_gameinfo._botsmanager.addBotListener( this );
		setDrivers( this,  _gameinfo, isItMasterBotDriver() );
	}

	public boolean isItMasterBotDriver() {
		return  _gameinfo.isItMasterBotDriver( this );
	}

	public InfoBot getInfoBot() {
		return fBot;
	}

	public String getName() {
		return fBot.getName();
	}

	public long getTime() {
		return _gameinfo.getTime();
	}

	public double getEnergy() {
		return fBot.getEnergy();
	}

	public double getDanger( long time, Point2D.Double dP ){
		double dLbot = 1.0; // enemy bot normalization
		// TODO: dRadius = 200; helps with melee survival but it drops APS
		// TODO: may be I need to make it dynamic
		double dRadius = 100; // effective dangerous radius of enemy Bot
		double dL = 0;
		double dist = 0;
		double botEnergy = getEnergy();
		dLbot = 1.0 * botEnergy/100; // weaker bots are less dangerous
		dist = dP.distance( getPositionClosestToTime( time ) ) ;
		if ( (botEnergy > robocode.Rules.MIN_BULLET_POWER) && (enemyBots.size() > 1) ) {
			// The following call is CPU intensive, so we ask for it only
			// when we sure that above condition is satisfied.
			// Am I doing the compiler optimization job?
			if ( isThisPointCloserThanAnyEnemy(time, dP) ) {
				dRadius *= 2;
			}
		}
		long ticsSinceRoundStart = getTime() - physics.getRoundStartTime( getTime() );
		int numOfmyWaves =  _gameinfo._wavesManager.getWavesOfBot(this).size();
		if ( numOfmyWaves > 0 || ticsSinceRoundStart < 100 ) {
			// bot is actively firing
			dL += dLbot * Math.exp( - dist/dRadius );
		} else {
			// this bot does not fire no danger in it
			if ( botEnergy > robocode.Rules.MIN_BULLET_POWER ) {
				// the bot still might fire
				// so some safety distance is still required
				dL += dLbot * (
						// bot itself still repels
						Math.exp( - dist/dRadius ) //same as regular bot
						//Math.exp( - 5*dist/dRadius )
						// but it is no good to be far so there is an
						// attractive potential.
						// TODO: the attraction seems to be a bad idea
						// see score drop between v2.8 -> v2.9
						// so I comment it out. May be I should enable
						// it when a bot energy below something small
						// like 10 or 20.
						// Also there is the bot nammyung.ModelT 0.23
						// which fires only when enemy is close.
						// My score against this bot drops
						// when the attraction is on.
						// Attraction is disabled for now.
						//+ Math.exp ( - 4*Math.abs(dist - physics.MaxSeparationOnBattleField)/physics.MaxSeparationOnBattleField )
						);

			} else {
				// this bot has no energy for bullets
				// approach as close as you want
				dL += dLbot * (
						// the bot is harmless attack at will
						Math.exp ( - 4*Math.abs(dist - physics.MaxSeparationOnBattleField)/physics.MaxSeparationOnBattleField )
						);
			}
		}
		return dL;
	}

	public boolean isThisPointCloserThanAnyEnemy( long time, Point2D.Double dP ) {
		double dist2point = dP.distance( getPositionClosestToTime( time ) ) ;
		Point2D.Double thisBP = this.getPositionClosestToTime(time);
		for ( fighterBot eBot: this.getEnemyBots() ) {
			double distNew = thisBP.distance( eBot.getPosition() );
			if ( ( distNew < dist2point) ) {
				return false;
			}
		}
		return true;

	}

	public Point2D.Double getPosition() {
		return fBot.getPosition();
	}

	public Point2D.Double getPositionClosestToTime( long time ) {
		return fBot.getPositionClosestToTime( time );
	}

	public botStatPoint getStatClosestToTime(long time) {
		return fBot.getStatClosestToTime( time );
	}

	public double getHeadingDegrees() {
		return fBot.getHeadingDegrees();
	}

	public int getNumEnemyAlive() {
		return _gameinfo.getNumEnemyAlive();
	}

	public long getLastSeenTime() {
		return fBot.getLast().getTime();
	}

	public gameInfo getGameInfo() {
		return _gameinfo;
	}

	public LinkedList<fighterBot> getEnemyBots() {
		return new LinkedList<fighterBot>(enemyBots.values());
	}

	public LinkedList<fighterBot> getAllKnownEnemyBots() {
		return new LinkedList<fighterBot>(allKnownEnemyBots.values());
	}


	public LinkedList<waveWithBullets> getEnemyWaves() {
		return enemyWaves;
	}

	public LinkedList<waveWithBullets> getMyWaves() {
		return myWaves;
	}

	public void removeBulletsOutsideOfHitRegion() {
		// this suppose to help in early stage of the game
		// by counting bullets which will not hit target
		// after some wave propagation.
		// There is no need to do it every tic
		// FIXME: make decision based on number of fired wave
		if ( isItMasterBotDriver() && getTime() < physics.ticTimeFromTurnAndRound(200, 0) ) {
			profiler.start("removeBulletsOutsideOfHitRegion");
			for ( waveWithBullets wB : getMyWaves() ) {
				wB.removeBulletsOutsideOfHitRegion( getTime() );
			}
			profiler.stop("removeBulletsOutsideOfHitRegion");
		}
	}

	public void initTic() {
		timer t = new timer( cpuManager.getCpuConstant() );
		//logger.dbg("fighterBot.initTic for " + getName());
		profiler.start("fighterBot.initTic " + getName());
		//logger.dbg( "Time left after removeBulletsOutsideOfHitRegion = " + profiler.profTimeString( t.timeLeft() ) );
		_gunManager.initTic();
		//logger.dbg( "Time left after _gunManager.initTic = " + profiler.profTimeString( t.timeLeft() ) );
		processScheduledEnergyDrop();
		//logger.dbg( "Time left after processScheduledEnergyDrop = " + profiler.profTimeString( t.timeLeft() ) );
		_motion.initTic();
		//logger.dbg( "Time left after _motion.initTic = " + profiler.profTimeString( t.timeLeft() ) );
		if ( isItMasterBotDriver() && (this.getGameInfo().fightType().equals("1on1") || this.getGameInfo().fightType().equals("melee1on1")) ) {
			// At large distances usually the 1st wave hits 
			// when two others are in the air.
			// This gives slight extra stats info about bullets
			// which miss even before wave hits the enemy
			// This is low priority operation, 
			// and needed only about first 100 waves
			removeBulletsOutsideOfHitRegion();
		}
		profiler.stop("fighterBot.initTic " + getName());
	}

	public void initBattle() {
		myScore = 0;
		enemyScore = 0;
		_gunManager.initBattle();
	}

	protected void processScheduledEnergyDrop() {
		profiler.start("processScheduledEnergyDrop");
		double eDrop = scheduledEdrop;
		if ( !isItMasterBotDriver() ) {
			// here we try to detect enemy wave fired
			double safetyMargin = 0.02;
			if ( eDrop >= robocode.Rules.MIN_BULLET_POWER - safetyMargin ) {
				// wave/bullet is fired
				// FIXME: be smarter about it: check for collisions with walls
				// Enemy energy drop due to fire is detected by one tic later thus -1
				eDrop = Math.min( eDrop, robocode.Rules.MAX_BULLET_POWER );
				wave w = new wave( getInfoBot(), getTime()-1, eDrop );
				_gameinfo._wavesManager.add( w );
				//logger.dbg(getName() + " fired bullet with energy = " + eDrop );
			}
		}
		scheduledEdrop = 0;
		profiler.stop("processScheduledEnergyDrop");
	}

	public void manage() {
		profiler.start("fighterBot.manage " + getName());
		_radar.manage();
		_motion.manage();
		_gunManager.manage();
		profiler.stop("fighterBot.manage " + getName());
	}

	public basicMotion getMotion() {
		return _motion;
	}

	public gunManager getGunManager() {
		return _gunManager;
	}

	public baseRadar getRadar() {
		return _radar;
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot tBot, long time, double bulletEnergy ) {
		// this method is called when a master bot detects enemy fire
		// since our concern is the master bot survival we do some checks
		
		// how this bot fires to the target bot
		LinkedList<firingSolution> fSolutions = new LinkedList<firingSolution>();
		LinkedList<baseGun> gunList = _gunManager.getGunList();

		if ( Math.random() < .25 ) {
			// sometimes we expect any bot to fire
		} else {
			if ( !tBot.getName().equals( this.getGunManager().getClosestTarget().getName() ) && getGameInfo().getNumEnemyAlive() >=4 ) {
				// target bot bot is not the closest
				// it is unlikely firing bot would use it as a target.
				return fSolutions;
			}
		}

		LinkedList<firingSolution> gunFSs = null;
		fSolutions = _gunManager.getFiringSolutions( _gameinfo.getFighterBot(tBot.getName()), time, bulletEnergy);

		// renormalizing the danger of solutions
		double maxQ = -1e6; 
		double q;

		// master bots solutions should be compared with unknownGun
		// unknownGun performance shows if we are guessing enemy's guns right.
		String2D keyUnknownGun = new String2D( "unknownGun", tBot.getName() );
		if ( !isItMasterBotDriver() ) {
			maxQ = _gunManager.getUnknownGunPerformanceAgainstBot( _gameinfo.getFighterBot(tBot.getName()) );
		}
		for ( firingSolution f: fSolutions) {
			q = Math.abs( f.getQualityOfSolution() );
			if ( q > maxQ ) {
				maxQ = q;
			}
		}
		for ( firingSolution f: fSolutions) {
			q = f.getQualityOfSolution()/maxQ;
			f.setQualityOfSolution(q);
		}

		return fSolutions;
	}

	public boolean isItMyWave(wave w) {
		return ( fBot.getName().equals( w.firedBot.getName() ) );
	}

	public void waveAdded(wave w) {
		if ( !isItMyWave(w) ) {
			// make with bullets from enemy Bot
			String enemyName =  w.firedBot.getName() ;
			fighterBot eBot = enemyBots.get( enemyName );
			if ( eBot == null ) {
				// FIXME: looks like this happens at the beginning of a game
				// if I hit yet invisible to a radar bot.
				logger.error("this should not happen: I was asked to add a wave for yet unknown bot " + enemyName );
				return;
			}
			logger.noise("bot " + fBot.getName() + " added enemy wave from " + enemyName );
			_gunManager.incrFiredByEnemy( enemyName ); 
			if ( eBot.isItMasterBotDriver() ) {
				// we do  not mess with master bot waves
				// enemy bot do not count or use them
				return;
			}
			waveWithBullets wB = new waveWithBullets( w, eBot.getGunManager().getGuessFactosrBinNum() );
			wB.setTargetBot( this );
			// debug flattener GF starts
			// assigns a random "safety" within MEA
			//double[] gfA = eBot.getGunManager().getGuessFactors( this.getName() );
			//int goodGF= (int) (gfA.length*Math.random() );
			//for (int i=0; i < gfA.length; i++) {
				//gfA[i] = 1-Math.exp( - Math.abs(i-goodGF)/5);
			//}
			//wB.copyGFarray( gfA );
			// debug flattener gun ends

			double[] gfA=eBot.getGunManager().calcMyWaveGFdanger( this, w.getFiredTime(), w.getBulletEnergy() );
			wB.copyGFarray( gfA );

			LinkedList<firingSolution> fSolutions = eBot.getFiringSolutions( fBot, w.getFiredTime(), w.getBulletEnergy() );
			wB.addFiringSolutions( fSolutions );
			for ( firingSolution fS: fSolutions ) {
				eBot.getGunManager().incrFiredAtEnemyByGun(fS);
			}
			// unknown gun does not make solutions so it had to be forced in
			eBot.getGunManager().incrFiredAtEnemyByGun(new unknownGun(), fBot);
			if ( fSolutions.size() == 0 ) {
				// this wave has no threat
				// ignoring it
				return; 
			}
			// make safety corridors in this wave from my bullets
			for ( firingSolution fS : getMyRealBullets() ) {
				wB.addSafetyCorridor( fS );
			}

			// add random safety corridor
			//double rAspread = 2*physics.calculateMEA(wB.getBulletSpeed())*(Math.random()-0.5);
			//double rA =  wB.getHeadOnAngle( fBot ) + rAspread;
			//rA = math.angleNorm360( rA-2 );
			//safetyCorridor sCrand = new safetyCorridor( rA, rA+4);
			//wB.removeFiringSolutionsInSafetyCorridor( sCrand );
			//wB.addToSafetyCorridors( sCrand );
			
			wB.calcCombineDanger();
			enemyWaves.add(wB);
			// calculate time when wave hits us if we do not move
			long hitTime = (long) (w.getFiredTime() + w.getTimeToReach( fBot.getPosition() ) );

			long safetyMargin = 5;
			hitTime += safetyMargin;
			long ticsToHit = (long) hitTime - getTime();
			if ( ticsToHit  > 0 ) {
				// this wave will reach us at hitTime in the future
				if ( hitTime > latestWaveHitTime ) {
					latestWaveHitTime = hitTime;
				}
				_motion.needToRecalculate = true;
				_motion.predictionEndTime = hitTime ;
			}
		} else {
			firedCount++;
			_gunManager.incrFiredCount();
			_gunManager.setLastFiredBullet( w.getBulletEnergy() );
			//logger.dbg("Detecting my own wave");
		}
	}

	public LinkedList<firingSolution> getMyRealBullets() {
		LinkedList<firingSolution> realBulletList = new LinkedList<firingSolution>();
		for (waveWithBullets wB : getMyWaves() ) {
			firingSolution fS = wB.getRealFiringSolution();
			if ( fS != null ) {
				realBulletList.add( fS );
			}
		}
		return realBulletList;
	}

	public void reportStats() {
		logger.routine("--- bot " + getName() + " stats:");
		if ( isItMasterBotDriver() ) {
			_motion.reportStats();
		}
		_gunManager.reportStats();
	}

	public void checkMyWaveForHits( waveWithBullets w ) {
		for ( firingSolution fS : w.getFiringSolutions() ) {
			if ( fS.isMyWavePassedOverTargetFlag() ) {
				_gunManager.logHitOrMissForMyFS( fS );
			}
		}
	}

	public void waveRemoved(wave w) {
		if ( !isItMyWave(w) ) {
			// going over enemy waves
			for ( waveWithBullets eW: enemyWaves ) {
				if ( eW.equals( w ) ) {
					enemyWaves.remove(eW);
					logger.noise(fBot.getName() + ": Enemy( " + eW.getFiredBot().getName() + ")  wave is removed");
					break;
				}
			}
		} else {
			// going over my waves
			for ( waveWithBullets mW: myWaves ) {
				if ( mW.equals( w ) ) {
					checkMyWaveForHits( mW );
					myWaves.remove(mW);
					logger.noise(fBot.getName() + ": my wave is removed");
					break;
				}
			}
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		_radar.onScannedRobot(e);
	}

	public void onRobotDeath(RobotDeathEvent e) {
		_radar.onRobotDeath(e);
	}

	public void onScannedRobot(InfoBot b) {
		if ( !getName().equals( b.getName() ) ) {
			logger.noise("Fighter bot " + getName() + " scanned " + b.getName() );
			enemyBots.put( b.getName(),  _gameinfo.liveBots.get( b.getName() ) );
			allKnownEnemyBots.put( b.getName(), _gameinfo.liveBots.get( b.getName() ) );
		} else {
			// the scanned bot is this one
			double eDrop = b.energyDrop();
			scheduledEdrop += eDrop;
		}
	}

	public void onRobotDeath(InfoBot b){
		enemyBots.remove( b.getName() ) ;
	}

	public void removeEnemyWaveWithBullet( fighterBot eBot, Bullet eBullet ){
		Point2D.Double interceptP = new Point2D.Double( eBullet.getX(), eBullet.getY() );
		for ( waveWithBullets eW: enemyWaves ) {
			if ( eW.getFiredBot().getName().equals(  eBullet.getName() ) ) {
				// this bullet from this bot which fired this wave
				double interceptTime = eW.getTimeToReach( interceptP );
				long travelTime = getTime() - eW.getFiredTime();
				if ( Math.abs( travelTime - interceptTime ) <=1.01 ) {
					// 0.01 added to bypass rounding errors
					// this is the wave which has eBullet
					
					// now we know which way enemy fires!
					// so we register it with realHitsGun kdTree
					// so we can avoid it.
					// streakly speaking it should be called realFireGF
					// since it did not hit the bot but the bullet
					double gf = eW.getAngleGF( eBullet.getHeading() ); 
					//logger.dbg(this.getTime() + ": " + this.getName() + " hit bullet of " + eBot.getName() + " at its GF = " +gf + " with real bullet");
					eBot.getGunManager().addRealHitGF( gf,  this.getInfoBot(), eW.getFiredTime(), eBullet.getPower() );
					// FIXME: it would be good idea to update all danger GF for already flying waves towards thisBot of the enemy bot (eBot) according to this new information

					// the wave  carries no danger, so we remove it
					enemyWaves.remove(eW);
					break;
				}

			}
		}
	}

	// our bullet hit a bullet from another bot
	public void onBulletHitBullet(fighterBot eBot, BulletHitBulletEvent e) {
		_gunManager.onBulletHitBullet( eBot );
		Bullet eBullet = null;
		if ( isItMasterBotDriver() ) {
			eBullet = e.getHitBullet();
		} else {
			eBullet = e.getBullet();
		}
		// removing enemy's intercepted wave
		removeEnemyWaveWithBullet( eBot, eBullet );

		// keep my wave assuming it was random hit and not a bullet shield
	}

	// someone hit the master bot
	public void onHitByBullet(HitByBulletEvent e) {
		_gunManager.onHitByBullet(e);
		if ( isItMasterBotDriver() ) {
			Bullet b = e.getBullet();
			double bulletEnergy = b.getPower();
			double bDamage = Rules.getBulletDamage(bulletEnergy);
			double bHitEnergyBonus = Rules.getBulletHitBonus(bulletEnergy);
			enemyScore += physics.bulletScoreBonusEnergy( bulletEnergy);
		}

	}

	// master bot bullet hit someone
	public void  onBulletHit(BulletHitEvent e) {
		if ( getName().equals( e.getName() ) ) {
			// this bot was hit
			// FIXME we can use bullet hit position to get some info about bot
			// if radar is looking at different direction at this tick
			double bulletEnergy = e.getBullet().getPower();
			// subtract from potential fired bullet energy
			// bullet damage for this bot
			scheduledEdrop -= physics.bulletDamageByEnergy( bulletEnergy );
		}
		if ( isItMasterBotDriver() ) {
			double bulletEnergy = e.getBullet().getPower();
			myScore += physics.bulletScoreBonusEnergy( bulletEnergy);

		}
		_gunManager.onBulletHit(e);
	}

	public void onWavePassingOverBot( wave w, InfoBot bot ) {
		profiler.start("fighterBot " + getName());
		profiler.start("onWavePassingOverBot");
		if ( w.getFiredBot().getName().equals( getName() ) ) {
			_gunManager.onMyWavePassingOverBot( w, bot );
		}
		if ( bot.getName().equals( getName() ) ) {
			_gunManager.onWavePassingOverMe( w );
		}
		if ( !bot.getName().equals(getName()) && !w.getFiredBot().getName().equals(getName()) ) {
			// someone else wave passing over other bot
			// FIXME: think about possible energy drop detection
			//        to avoid false fired wave generation
			profiler.start("addSafetyCorridor from shadow of " + bot.getName());
			w.addSafetyCorridor( getGameInfo().getFighterBot( bot.getName() ) );
			for ( waveWithBullets wB: getEnemyWaves() ) {
				if ( w.equals( wB) ) {
					wB.addSafetyCorridor( getGameInfo().getFighterBot( bot.getName() ) );
				}
			}
			profiler.stop("addSafetyCorridor from shadow of " + bot.getName());
		}
		profiler.stop("onWavePassingOverBot");
		profiler.stop("fighterBot " + getName());
	}

	public void drawThisBot( Graphics2D g, long timeNow ) {
		double size = 40;
		Point2D.Double  p = fBot.getPositionClosestToTime( timeNow );
		if ( p != null ) {
			g.setColor(new Color(0x00, 0x00, 0xff, 0x80));
			graphics.drawSquare( g, p, size );
		}
	}

	public void drawEnemyBot( Graphics2D g, long timeNow, fighterBot eB ) {
		double size = 40;
		Point2D.Double  p = eB.fBot.getPositionClosestToTime( timeNow );
		if ( p != null ) {
			g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
			graphics.drawSquare( g, p, size );
		}
	}

	public void onPaint( Graphics2D g, long timeNow ) {
		if ( isItMasterBotDriver() ) {
			// draw itself
			drawThisBot( g, timeNow );
			// draw enemy waves
			for ( waveWithBullets eW: enemyWaves ) {
				eW.onPaint( g, timeNow );
			}
			// draw my waves
			for ( waveWithBullets w: myWaves ) {
				w.onPaint( g, timeNow );
			}
			// draw known enemy bots
			for ( fighterBot eB: getEnemyBots() ) {
				drawEnemyBot( g, timeNow, eB );
			}
			// draw motion
			_motion.onPaint( g );

			// draw gun manager
			_gunManager.onPaint( g );
		}
	}

}
