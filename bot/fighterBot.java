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
		_motion.initTic();
	}

	public void manage() {
		_radar.manage();
		_motion.manage();
		_gunManager.manage();
	}

	public basicMotion getMotion() {
		return _motion;
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot tBot, long time, double bulletEnergy ) {
		// how this bot fires to the target bot
		LinkedList<firingSolution> fSolutions = new LinkedList<firingSolution>();

		LinkedList<firingSolution> gunFSs = null;
		// FIXME: here should be loop over all available guns
		baseGun g = new headOnGun();
		gunFSs =  g.getFiringSolutions( fBot, tBot, time, bulletEnergy ) ;
		fSolutions.addAll( gunFSs );

		g = new linearGun();
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
			waveWithBullets wB = new waveWithBullets( w );
			LinkedList<firingSolution> fSolutions = eBot.getFiringSolutions( fBot, w.getFiredTime(), w.getBulletEnergy() );
			for ( firingSolution fS: fSolutions ) {
				wB.addFiringSolution(fS);
			}
			enemyWaves.add(wB);
			_gunManager.incrFiredByEnemy( enemyName ); 
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
		}
	}

	public void onRobotDeath(InfoBot b){
		enemyBots.remove( b.getName() ) ;
	}

	// someone hit the master bot
	public void onHitByBullet(HitByBulletEvent e) {
		_gunManager.onHitByBullet(e);
	}

	// master bot bullet hit someone
	public void  onBulletHit(BulletHitEvent e) {
		_gunManager.onBulletHit(e);
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
				//w.onPaint( g, timeNow );
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
