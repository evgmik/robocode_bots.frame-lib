// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.wave.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

import robocode.*;
import robocode.util.*;
import robocode.Rules.*;


public class gunManager implements gunManagerInterface {
	public LinkedList<baseGun> gunList = new LinkedList<baseGun>();
	public fighterBot myBot;
	fighterBot targetBot = null;
	protected int  firedCount = 0;
	double firingSolutionQualityThreshold = .5;
	long fireAtTime = -9999; // gun manager will set this time
	LinkedList<firingSolution> firingSolutions = new LinkedList<firingSolution>();
	firingSolution bestFiringSolution = null;

	public HashCounter<String> hitByOther = new HashCounter<String>();
	public HashCounter<String> hitByMe = new HashCounter<String>();
	public HashCounter<String> firedAt = new HashCounter<String>();
	public HashCounter<String> firedByEnemy = new HashCounter<String>();

	// my gun stats
	public HashCounter<String2D> hitByMyGun = new HashCounter<String2D>();
	public HashCounter<String2D> hitByEnemyGun = new HashCounter<String2D>();

	public	gunManager() {
		gunList = new LinkedList<baseGun>();
		gunList.add( new linearGun() );
	}

	public	gunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public void manage() {
	}

	// someone hit the master bot
	public void onHitByBullet(HitByBulletEvent e) {
		String trgtBotName = myBot.getGameInfo().getMasterBot().getName();
		String fireBotName = e.getName();

		incrHitCounts( trgtBotName, fireBotName );
	}

	// master bot bullet hit someone
	public void  onBulletHit(BulletHitEvent e) {
		String trgtBotName = e.getName();
		String fireBotName = myBot.getGameInfo().getMasterBot().getName();
		incrHitCounts( trgtBotName, fireBotName );
	}
	
	public void incrHitCounts( String trgtBotName, String fireBotName ) {
		if ( myBot.getName().equals( trgtBotName ) ) {
			// this bot is hit by other
			hitByOther.incrHashCounter( fireBotName );
		}
		if ( myBot.getName().equals( fireBotName ) ) {
			// this bot hit someone
			hitByMe.incrHashCounter( trgtBotName );
		}
	}

	public void reportHitByOther(){
		logger.routine("Hit me count by the following bot(s)");
		for ( fighterBot fB: myBot.getAllKnownEnemyBots() ) {
			String bName = fB.getName();
			logger.routine( " " + bName + ": " + logger.hitRateFormat( hitByOther.getHashCounter( bName ) , firedByEnemy.getHashCounter( bName ) ) );
		}
	}

	public void onMyWavePassingOverBot( wave w, InfoBot bot ) {
		// FIXME: update stats
		// FIXME: firing solution need to know target otherwise
		//        we count bullets directed to someone else
		long time = myBot.getTime();
		String  enemyName = bot.getName();
		Point2D.Double botPos = bot.getPositionClosestToTime( time );

		for ( waveWithBullets wB: myBot.myWaves ) {
			if ( w.equals( wB) ) {
				LinkedList<firingSolution> hitSolutions = wB.getFiringSolutionsWhichHitBotAt( botPos,  time );
				for ( firingSolution fS : hitSolutions ) {
					String gunName = fS.getGunName();
					String2D key = new String2D( gunName, enemyName );
					hitByMyGun.incrHashCounter( key );
					wB.removeFiringSolution( fS );
				}
			}
		}
	}

	public void onWavePassingOverMe( wave w ) {
		long time = myBot.getTime();
		Point2D.Double botPos = myBot.getPosition( ); // time is now
		for ( waveWithBullets wB: myBot.enemyWaves ) {
			if ( w.equals( wB) ) {
				LinkedList<firingSolution> hitSolutions = wB.getFiringSolutionsWhichHitBotAt( botPos,  time );
				for ( firingSolution fS : hitSolutions ) {
					String gunName = fS.getGunName();
					String2D key = new String2D( gunName, wB.getFiredBot().getName() );
					hitByEnemyGun.incrHashCounter( key );
					wB.removeFiringSolution( fS );
				}
			}
		}
	}

	public void reportHitByMe(){
		logger.routine("hit rate for the following bot(s) out of " + firedCount + " shots");
		for ( fighterBot fB: myBot.getAllKnownEnemyBots() ) {
			String bName = fB.getName();
			Integer fCnt;
			fCnt = firedAt.getHashCounter( bName );
			if ( fCnt == 0 ) {
				// we have no clue to whom we fired
				// using total firedCount
				fCnt = firedCount;
			}
			logger.routine( " " + bName + ": " + logger.hitRateFormat( hitByMe.getHashCounter( bName ), fCnt ) );
		}
	}

	public void reportMyGunStats() {
		// FIXME: some wave are still flying and are not fully accounted
		logger.routine("  My virtual gun stats");
		for( String2D key: hitByMyGun.keySet() ) {
			logger.routine("    " + key.getX() + " hit bot " + key.getY()
				       + " " + logger.hitRateFormat( hitByMyGun.getHashCounter( key ), firedAt.getHashCounter( key.getY() ) ) );
		}
	}

	public void reportEnemyGunStats() {
		// FIXME: some wave are still flying and are not fully accounted
		logger.routine("  Enemies virtual gun stats");
		for( String2D key: hitByEnemyGun.keySet() ) {
			logger.routine("    " + key.getX() + " of bot " + key.getY() + " hit me " + logger.hitRateFormat( hitByEnemyGun.getHashCounter( key ), firedByEnemy.getHashCounter( key.getY() ) ) );
		}
	}

	public void reportStats() {
		if ( myBot.isItMasterBotDriver() ) {
			reportHitByOther();
			reportEnemyGunStats();
		}
		reportHitByMe();
		if ( myBot.isItMasterBotDriver() ) {
			reportMyGunStats();
		}
	}

	public void incrFiredCount() {
		firedCount++;
	}

	public void incrFiredByEnemy(String enemyName) {
		firedByEnemy.incrHashCounter( enemyName );
	}


	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
		if ( targetBot != null ) {
			double R = 50;
			graphics.drawCircle( g, targetBot.getPosition(), R );
		}
	}
}
