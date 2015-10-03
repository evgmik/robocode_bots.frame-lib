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
	double firingSolutionQualityThreshold = .005;
	long fireAtTime = -9999; // gun manager will set this time
	LinkedList<firingSolution> firingSolutions = new LinkedList<firingSolution>();
	firingSolution bestFiringSolution = null;

	public HashCounter<String> hitByOther = new HashCounter<String>();
	public HashCounter<String> hitByMe = new HashCounter<String>();
	public HashCounter<String> firedAt = new HashCounter<String>();
	public HashCounter<String> firedByEnemy = new HashCounter<String>();
	public HashCounter<String> hitBullet = new HashCounter<String>();

	// my gun stats
	public HashCounter<String2D> hitByMyGun = new HashCounter<String2D>();
	public HashCounter<String2D> hitByEnemyGun = new HashCounter<String2D>();
	public HashCounter<String2D> firedAtEnemyByGun = new HashCounter<String2D>();

	protected int numGuessFactorBins = 31;
	protected HashMap<String, int[]> guessFactorsMap = new HashMap<String, int[]>();

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

	public fighterBot getTarget() {
		return targetBot;
	}

	public fighterBot getClosestTarget() {
		double dist2closestBot = 1e6; // something very large
		long infoDelayTimeThreshold = (long) (360/robocode.Rules.RADAR_TURN_RATE + 1);
		double distNew;
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			distNew = myBot.getPosition().distance( eBot.getPosition() );
			if ( ( distNew < dist2closestBot) && ((myBot.getTime() - eBot.getLastSeenTime()) < infoDelayTimeThreshold) ) {
				dist2closestBot = distNew;
				targetBot = eBot;
			}
		}
		return targetBot;
	}

	// master bot bullet hit someone
	public void  onBulletHit(BulletHitEvent e) {
		String trgtBotName = e.getName();
		String fireBotName = myBot.getGameInfo().getMasterBot().getName();
		incrHitCounts( trgtBotName, fireBotName );
	}
	
	// our bullet hit a bullet from another bot
	public void onBulletHitBullet(fighterBot eBot) {
		hitBullet.incrHashCounter( eBot.getName() );
	}

	// checks if enemy employs bullet shield
	public boolean isBulletShieldDetected( String eName ) {
		int fC = firedByEnemy.getHashCounter( eName );
		double erate = math.eventRate( hitBullet.getHashCounter( eName ), fC );
		int confedenceNum = 10;
		double detectionTshreshold = 0.2;
		if ( (fC >= confedenceNum) && ( erate >= detectionTshreshold ) ) {
			return true;
		}
		return false;
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

		// calculate myBot targeting GF
		updateHitGuessFactor( bot, w.getFiringGuessFactor(bot, time) );

		// count the wave with bullets
		for ( waveWithBullets wB: myBot.myWaves ) {
			if ( w.equals( wB) ) {
				LinkedList<firingSolution> hitSolutions = wB.getFiringSolutionsWhichHitBotAt( botPos,  time );
				for ( firingSolution fS : hitSolutions ) {
					if ( fS.getTargetBotName().equals(enemyName) ) {
						// this bullet is intended for this bot
						String gunName = fS.getGunName();
						String2D key = new String2D( gunName, enemyName );
						hitByMyGun.incrHashCounter( key );
						wB.removeFiringSolution( fS );
					} else {
						// FIXME: count somehow unintentional hits
					}
				}
			}
		}
	}

	public int[] getGuessFactors( String  botName ) {
                if ( !guessFactorsMap.containsKey( botName ) ) {
			int[] guessFactorBins = new int[numGuessFactorBins];
			guessFactorsMap.put( botName, guessFactorBins );
                }
                int[] gfBins = guessFactorsMap.get( botName );
		return gfBins;
	}

	public int getGuessFactosrBinNum() {
		return numGuessFactorBins;
	}

	public void updateHitGuessFactor( InfoBot bot, double gf ) {
		int i = (int)math.gf2bin( gf, numGuessFactorBins );
		i = (int)math.putWithinRange( i, 0, (numGuessFactorBins-1) );
		int[] gfBins = getGuessFactors( bot.getName() );
		gfBins[i]++;
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
		logger.routine("  My virtual gun hit rate stats stats");
		String str = "  ";
		// make headers
		str += String.format( "%25s", "enemy name" );
		for ( baseGun g: gunList ) {
			str += String.format( "%25s", g.getName() );
		}
		logger.routine( str );
		for ( fighterBot b: myBot.getAllKnownEnemyBots() ) {
			str = "  ";
			String enemyName = b.getName();
			str += String.format( "%25s", enemyName );
			for ( baseGun g: gunList ) {
				String2D key = new String2D( g.getName(), enemyName );
				str += String.format( "%25s", logger.hitRateFormat( hitByMyGun.getHashCounter( key ), firedAtEnemyByGun.getHashCounter( key ) ) );

			}
			logger.routine( str );
		}
	}

	public void reportEnemyGunStats() {
		// FIXME: some wave are still flying and are not fully accounted
		logger.routine("  Enemies virtual gun stats");
		for( String2D key: hitByEnemyGun.keySet() ) {
			logger.routine("    " + key.getX() + " of bot " + key.getY() + " hit me " + logger.hitRateFormat( hitByEnemyGun.getHashCounter( key ), firedByEnemy.getHashCounter( key.getY() ) ) );
		}
	}

	public void reportBulletHitBullet() {
		for( String key: hitBullet.keySet() ) {
			logger.routine( "bot " + key + " intercepted my bullet " + logger.hitRateFormat( hitBullet.getHashCounter( key ), firedByEnemy.getHashCounter( key ) ) );
		}
	}

	public void reportGFStats() {
		for( String key: guessFactorsMap.keySet() ) {
			logger.routine( "bot " + key + " seen at GF: " + Arrays.toString(guessFactorsMap.get(key)) );
		}
	}

	public void reportStats() {
		if ( myBot.isItMasterBotDriver() ) {
			reportHitByOther();
			reportEnemyGunStats();
		}
		reportHitByMe();
		reportGFStats();
		if ( myBot.isItMasterBotDriver() ) {
			reportBulletHitBullet();
			reportMyGunStats();
		}
	}

	public void incrFiredCount() {
		firedCount++;
	}

	public void incrFiredAtEnemyByGun(firingSolution fS) {
		String2D key = new String2D( fS.getGunName(), fS.getTargetBotName() );
		firedAtEnemyByGun.incrHashCounter( key );
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
