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
		return  getName().equals( _gameinfo.getMasterBot().getName() );  
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
		double dRadius = 100; // effective dangerous radius of enemy Bot
		double dL = 0;
		double dist = 0;
		dLbot = 1.0 * getEnergy()/100; // weaker bots are less dangerous
		dist = dP.distance( getPositionClosestToTime( time ) ) ;
		dL += dLbot * Math.exp( - dist/dRadius );
		return dL;
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

	public void initTic() {
		processScheduledEnergyDrop();
		_motion.initTic();
	}

	protected void processScheduledEnergyDrop() {
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
			}
		}
		scheduledEdrop = 0;
	}

	public void manage() {
		_radar.manage();
		_motion.manage();
		_gunManager.manage();
	}

	public basicMotion getMotion() {
		return _motion;
	}

	public gunManager getGunManager() {
		return _gunManager;
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot tBot, long time, double bulletEnergy ) {
		// this method is called when a master bot detects enemy fire
		// since our concern is the master bot survival we do some checks
		
		// how this bot fires to the target bot
		LinkedList<firingSolution> fSolutions = new LinkedList<firingSolution>();

		if ( !tBot.getName().equals( this.getGunManager().getClosestTarget().getName() ) && getGameInfo().getNumEnemyAlive() >=4 ) {
			// target bot bot is not the closest
			// it is unlikely firing bot would use it as a target.
			return fSolutions;
		}

		LinkedList<firingSolution> gunFSs = null;
		// FIXME: here should be loop over all available guns
		baseGun g = new headOnGun();
		gunFSs =  g.getFiringSolutions( fBot, tBot, time, bulletEnergy ) ;
		fSolutions.addAll( gunFSs );

		if ( getGameInfo().getNumEnemyAlive() >= 7 ) {
			return fSolutions;
		}

		g = new linearGun();
		gunFSs =  g.getFiringSolutions( fBot, tBot, time, bulletEnergy ) ;
		fSolutions.addAll( gunFSs );
		if ( getGameInfo().getNumEnemyAlive() >= 5 ) {
			return fSolutions;
		}


		// now we have small enough enemy number to take all the guns
		g = new circularGun();
		gunFSs =  g.getFiringSolutions( fBot, tBot, time, bulletEnergy ) ;
		fSolutions.addAll( gunFSs );
		
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
			logger.noise("bot " + fBot.getName() + " added enemy wave from " + enemyName );
			_gunManager.incrFiredByEnemy( enemyName ); 
			waveWithBullets wB = new waveWithBullets( w );
			LinkedList<firingSolution> fSolutions = eBot.getFiringSolutions( fBot, w.getFiredTime(), w.getBulletEnergy() );
			for ( firingSolution fS: fSolutions ) {
				wB.addFiringSolution(fS);
			}
			if ( fSolutions.size() == 0 ) {
				// this wave has no threat
				// ignoring it
				return; 
			}
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
			//logger.dbg("Detecting my own wave");
		}
	}

	public void reportStats() {
		logger.routine("--- bot " + getName() + " stats:");
		if ( isItMasterBotDriver() ) {
			_motion.reportStats();
		}
		_gunManager.reportStats();
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

	// our bullet hit a bullet from another bot
	public void onBulletHitBullet(fighterBot eBot, BulletHitBulletEvent e) {
		_gunManager.onBulletHitBullet( eBot );
		// removing enemy's intercepted wave

		// keep my wave assuming it was random hit and not a bullet shield
	}

	// someone hit the master bot
	public void onHitByBullet(HitByBulletEvent e) {
		_gunManager.onHitByBullet(e);
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
		_gunManager.onBulletHit(e);
	}

	public void onWavePassingOverBot( wave w, InfoBot bot ) {
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
		}
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
