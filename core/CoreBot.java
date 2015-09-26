package eem.frame.core;

import eem.botVersion;
import eem.frame.radar.*;
import eem.frame.motion.*;
import eem.frame.bot.*;
import eem.frame.wave.*;
import eem.frame.gameInfo.*;
import eem.frame.dangermap.*;
import eem.frame.misc.*;

import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import java.util.*;

import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

/**
 * CoreBot - very peaceful robot by Eugeniy E. Mikhailov
 */
public class CoreBot extends AdvancedRobot
{
	public Rules game_rules;
	double BodyTurnRate = game_rules.MAX_TURN_RATE;

	private botVersion botVer;
	public static gameInfo _gameinfo;

	public int numEnemyBotsAlive = 1; // we have at least one enemy in general
	public long initTicStartTime = 0;

	public Point2D.Double myCoord;
	double absurdly_huge=1e6; // something huge
	double desiredBodyRotationDirection = 0; // our robot body desired angle



	public long ticTime;
	public int roundCnt = 0;
	int nonexisting_coord = -10000;
	public int totalNumOfEnemiesAtStart = 0;
	public static int roundsWon = 0;
	public static int roundsLost = 0;
	public static int  finishingPlacesStats[] = null;
	public static int  skippedTurnStats[] = null;
	public static int  hitWallStats[] = null;
	public static int  hitByBulletStats[] = null;
	public static int bulletFiredCnt = 0;
	public static int bulletHitEnemyCnt = 0;
	public static int bulletHitEnemyBulletCnt = 0;
        public static int bulletHitByPredictedCnt = 0;	
	private static int numTicsWhenGunInColdState = 0;

	// logger staff
	private String logFileName = "CoreBot.log";
	public int verbosity_level=logger.log_debuging; // current level, smaller is less noisy
	private static RobocodeFileWriter fileWriter = null;
	private boolean appendToLogFlag = false; // TODO: make use of it
	public logger _log = null;

	public CoreBot() {
	}

	public void initBattle() {
		roundCnt = getRoundNum() + 1;
		// this part should be done only once and for all rounds
		if ( fileWriter == null ) {
			try {
				fileWriter = new RobocodeFileWriter( this.getDataFile( logFileName ) );
				_log = new logger(verbosity_level, fileWriter);
			} catch (IOException ioe) {
				System.out.println("Trouble opening the logging file: " + ioe.getMessage());
				_log = new logger(verbosity_level);
			}
		}

		physics.init(this);
		math.init(this);
		setColors(Color.red,Color.blue,Color.green); //colors of by bot
		botVer = new botVersion();

		totalNumOfEnemiesAtStart = getOthers();
		if ( finishingPlacesStats == null ) {
			finishingPlacesStats = new int[totalNumOfEnemiesAtStart+1];
		}

		if ( skippedTurnStats == null ) {
			skippedTurnStats = new int[getNumRounds()];
		}

		if ( hitWallStats == null ) {
			hitWallStats = new int[getNumRounds()];
		}

		if ( hitByBulletStats == null ) {
			hitByBulletStats = new int[getNumRounds()];
		}


		// the part below should be done for every round
		logger.routine("=========== Round #" + (roundCnt) + "=============");
		myCoord = new Point2D.Double( getX(), getY() );

		if ( _gameinfo == null ) {
			_gameinfo = new gameInfo(this);
		}
		_gameinfo.initBattle(this);

		// give ScannedRobotEvent almost the highest priority,
		// otherwise when I process bullet related events
		// I work with old enemy bots coordinates
		setEventPriority("ScannedRobotEvent", 98);
		initTicStartTime = System.nanoTime();
	}

	public void initTic() {

		numEnemyBotsAlive = getOthers();

		// gun, radar, and body are decoupled
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true); 

		setTicTime();

		logger.noise("----------- Bot version: " + botVer.getVersion() + "------- Tic # " + ticTime + " -------------");
		logger.noise("Game time: " + ticTime);
		logger.noise("Number of other bots = " + numEnemyBotsAlive);
		
		if ( numEnemyBotsAlive == 0 ) {
			//logger.dbg("Round is over");
		}

		myCoord.x = getX();
	       	myCoord.y = getY();

		_gameinfo.initTic();

	}

	private void setTicTime() {
		ticTime = this.getTime();
	}

	public long getTime() {
		// Robocode start every round with zero
		// to keep our own time increasing along the battle
		// we add to this time 100000*round_number
		// this big enough to separate rounds 
		return physics.ticTimeFromTurnAndRound( super.getTime(), getRoundNum() );
		//return ( super.getTime() + 100000*(getRoundNum()+1) ); 
	}

	public String fightType() {
		double survRatio = 1.0*numEnemyBotsAlive/totalNumOfEnemiesAtStart;
		if ( (numEnemyBotsAlive == 1) && (totalNumOfEnemiesAtStart == 1) )
			return "1on1";
		if ( (numEnemyBotsAlive == 1) && (totalNumOfEnemiesAtStart != 1) )
			return "melee1on1";
		if ( survRatio > 2./3. )
			return "melee";
		return "meleeMidle";
	}

	public double distTo(double x, double y) {
		double dx=x-myCoord.x;
		double dy=y-myCoord.y;

		return Math.sqrt(dx*dx + dy*dy);
	}

	public void run() {
		initBattle();

		while(true) {
			initTic() ;
			if ( getOthers() == 0 ) {
				//logger.dbg("Round is over");
			}

			//FIXME
			//_radar.setNeedToTrackTarget( _gun.doesItNeedTrackedTarget() );
			
			_gameinfo.run();
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		setTicTime();
		myCoord.x = getX();
	       	myCoord.y = getY();

		_gameinfo.onScannedRobot(e);

	}

	// someone bullet hit us
	public void onHitByBullet(HitByBulletEvent e) {
		_gameinfo.onHitByBullet(e);
		hitByBulletStats[getRoundNum()]++;
	}

	// our bullet hit someone
	public void  onBulletHit(BulletHitEvent e) {
		_gameinfo.onBulletHit(e);
		bulletHitEnemyCnt++;
	}

	// our bullet missed and hit the wall
	public void  onBulletMissed(BulletMissedEvent e) {
	}

	// our bullet hit one of theirs
	public void onBulletHitBullet(BulletHitBulletEvent e) {
		_gameinfo.onBulletHitBullet(e);
		bulletHitEnemyBulletCnt++;
	}

	public void onRobotDeath(RobotDeathEvent e) {
		_gameinfo.onRobotDeath(e);
	}

	public void onHitWall(HitWallEvent e) {
		hitWallStats[getRoundNum()]++;
		logger.dbg( "tic " + getTime() + ": shame I hit wall" );
	}
		
	public void onSkippedTurn(SkippedTurnEvent e) {
		skippedTurnStats[getRoundNum()]++;
		logger.routine("Skipped turn " + e.getSkippedTurn() + " reported at " + getTime() );
	}
	
	public void onPaint(Graphics2D g) {
		// If you run above you notice that time
		// is one tic ahead of the known bots stats.
		// This is quite a problem since even my own bot stats are not updated yet
		// in my data structures.
		// So do not be concerned with mis aligned locations of the bot
		_gameinfo.onPaint(g);
	}

	public void onWin(WinEvent  e) {
		setTicTime();
		//logger.dbg("onWin");
		roundsWon++;
		updateFinishingPlacesStats();
		_gameinfo.onWin(e);
		winOrLoseRoundEnd();
	}

	public void onDeath(DeathEvent e) {
		roundsLost++;
		updateFinishingPlacesStats();
		_gameinfo.onDeath(e);
		winOrLoseRoundEnd();
	}

	public void onRoundEnded(RoundEndedEvent e) {
		// this methods is called before onDeath or onWin
		// so we should not output any valuable stats here
		// if I want to see it at the end
		//logger.dbg("onRoundEnded");
		//winOrLoseRoundEnd();
	}

	public void updateFinishingPlacesStats() {
		int myWinLosePlace = getOthers();
		finishingPlacesStats[myWinLosePlace]++;
		logger.routine("Hit by bullet: " + Arrays.toString(hitByBulletStats) );
		logger.routine("Wall hits stats: " + Arrays.toString(hitWallStats) );
		logger.routine("Skipped turns stats: " + Arrays.toString(skippedTurnStats) );
		logger.routine("Hit rate stats: " + logger.hitRateFormat( bulletHitEnemyCnt, bulletFiredCnt ) );
		logger.routine("Bullet hit bullet stats:: " + logger.hitRateFormat( bulletHitEnemyBulletCnt, bulletFiredCnt ) );
		logger.routine("Rounds ratio of win/lose = " + roundsWon + "/" + roundsLost );
		logger.routine("Finishing places stats: " + Arrays.toString( finishingPlacesStats ) );
	}

	public void winOrLoseRoundEnd() {
		logger.routine( profiler.formatAll() );
	}

}
