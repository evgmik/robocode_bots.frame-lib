// -*- java -*-

package eem.frame.gameInfo;

import eem.frame.core.*;
import eem.frame.event.*;
import eem.frame.radar.*;
import eem.frame.motion.*;
import eem.frame.bot.*;
import eem.frame.wave.*;
import eem.frame.misc.*;

import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.*;

public class gameInfo implements botListener {
	public CoreBot myBot;
	public basicMotion _motion;
	public botsManager _botsmanager;
	public wavesManager _wavesManager;

	public static HashMap<String,fighterBot> liveBots = new HashMap<String, fighterBot>();
	public static HashMap<String,fighterBot> deadBots = new HashMap<String, fighterBot>();;


	public gameInfo(CoreBot bot) {
		logger.noise( "----- Creating gameInfo -----" );
		setMasterBot(bot);
		_wavesManager = new wavesManager(myBot);
		_botsmanager = new botsManager( myBot, this );
		_botsmanager.addBotListener( this );
	}

	public void setMasterBot( CoreBot b) {
		myBot = b;
	}

	public CoreBot getMasterBot() {
		return myBot;
	}

	public fighterBot getFighterBot(String name) {
		fighterBot b = null;
		b = liveBots.get(name);
		if (b != null)
			return b;
		b = deadBots.get(name);
		return b; // it is either null for unknown name or one of the dead ones
	}

	public void initBattle( CoreBot b) {
		setMasterBot( b );
	}

	public void initTic() {
		long timeNow = myBot.getTime();
		_botsmanager.initTic( timeNow );
		_wavesManager.initTic( timeNow );
		for( fighterBot fb: liveBots.values() ) {
			fb.initTic();
		}
	}

	public long getRoundNum() {
		return myBot.getRoundNum();
	}

	public long getTime() {
		return myBot.getTime();
	}

	public int getNumEnemyAlive() {
		return myBot.numEnemyBotsAlive;
	}

	public wavesManager getWavesManager() {
		return _wavesManager;
	}

	public void run() {
		for (fighterBot b : liveBots.values()) {
			b.manage();
		}
		myBot.execute();
	}

	public String fightType() {
		return myBot.fightType();
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		_botsmanager.onScannedRobot(e);
		for ( fighterBot b: liveBots.values() ) {
			b.onScannedRobot(e);
		}
	}

	public void onScannedRobot(InfoBot b) {
		// it would be nice it amont other listeners
		// but we create botListener with new fighterBot
		// so we reserve to use the function below
	}

	public void specialOnScannedRobot(InfoBot b) {
		// here we are getting results from _botsmanager
		// but this function is a hack since we cannot use
		// onScannedRobot(InfoBot b)
		String botName = b.getName();
		logger.noise("Time: " + getTime() + " Scanned bot " + botName );
		fighterBot knownBot = null;
	       	knownBot = liveBots.get(botName);
		if ( knownBot != null ) {
			// nothing to do
			return;
		}
	       	knownBot = deadBots.get(botName);
		if ( knownBot != null ) {
			// bot is known but among dead ones
			// ressurecting it
			logger.noise("game manager ressurecting " + botName );
			logger.noise("old ref " + deadBots.get( botName) );
			logger.noise("new ref " +  knownBot );
			liveBots.put( botName, knownBot );
			deadBots.remove( botName );
			return;
		}
		// this is newly discovered bot
		knownBot = new fighterBot( b, this );
		if ( knownBot == null ) {
			// should never happen
			logger.error("Something wery wrong! We should have got fighterBot for " + botName);
		}
		liveBots.put( botName, knownBot );
	}

	public void onRobotDeath(RobotDeathEvent e) {
		_botsmanager.onRobotDeath(e);
		for ( fighterBot b: liveBots.values() ) {
			b.onRobotDeath(e);
		}
	}

	public void onRobotDeath(InfoBot b){
		// here we are getting results from _botsmanager
		logger.noise( "gameInfo: bot " + b.getName() + " is dead" );
		String botName = b.getName();
		fighterBot dBot = liveBots.get(botName);
		deadBots.put( botName, dBot);
		liveBots.remove( botName );
		logger.noise( this.toString() );
	}

	public void onWin(WinEvent  e) {
		botsReportStats();
	}

	public void onDeath(DeathEvent e) {
		botsReportStats();
	}

	// our bullet hit someone
	public void  onBulletHit(BulletHitEvent e) {
		HashMap<String,fighterBot> allBots = getAllFighterBots(); 
		for ( fighterBot fB: allBots.values() ) {
			fB.onBulletHit(e);
		}
	}

	// our bullet hit one of theirs
	public void onBulletHitBullet(BulletHitBulletEvent e) {
		Bullet b = e.getHitBullet();
		String name = b.getName();
		fighterBot bot = getFighterBot( name );
		if ( bot == null )
			return;
		// valid bot
		// we increase call both: our master bot and enemy bot
		// 1st master bot
		getFighterBot( getMasterBot().getName() ).onBulletHitBullet( bot, e );
		// 2nd enemy bot
		bot.onBulletHitBullet( getFighterBot( getMasterBot().getName() ), e );
	}

	// someone bullet hit us
	public void onHitByBullet(HitByBulletEvent e) {
		HashMap<String,fighterBot> allBots = getAllFighterBots(); 
		for ( fighterBot fB: allBots.values() ) {
			fB.onHitByBullet(e);
		}
	}

	public void onWavePassingOverBot( wave w, InfoBot bot ) {
		for ( fighterBot fB: liveBots.values() ) {
			fB.onWavePassingOverBot( w, bot );
		}
	}

	public HashMap<String,fighterBot> getAllFighterBots() {
		HashMap<String,fighterBot> allBots = new HashMap<String, fighterBot>();
		allBots.putAll( liveBots);
		allBots.putAll( deadBots);
		return allBots;
	}

	public boolean isItMasterBotDriver( String bName ) {
		return  bName.equals( getMasterBot().getName() );  
	}

	public boolean isItMasterBotDriver( InfoBot b ) {
		return  isItMasterBotDriver( b.getName() );
	}

	public boolean isItMasterBotDriver( fighterBot b ) {
		return  isItMasterBotDriver( b.getName() );
	}

	public void botsReportStats() {
		HashMap<String,fighterBot> allBots = getAllFighterBots(); 

		fighterBot masterBot = null;
		for ( fighterBot fB: allBots.values() ) {
			if ( fB.getName().equals( myBot.getName() ) ) {
				// we will output master bot stats later
				masterBot = fB;
			} else {
				fB.reportStats();
			}
		}
		masterBot.reportStats();
	}

	public void onPaint( Graphics2D g ) {
		_botsmanager.onPaint(g);
		//_wavesManager.onPaint(g);
		long timeNow = myBot.getTime();
		for ( fighterBot fB: liveBots.values() ) {
			if ( fB.isItMasterBotDriver() ) {
				fB.onPaint( g, timeNow );
			}
		}
	}

	public String toString() {
		String str = "";
		str += "Game Info stats\n";
		str += " liveBots known = " + liveBots.size() + "\n";
		for (fighterBot b : liveBots.values()) {
			str += "  bot: " + b.getName() + "\n";
		}
		str += " deadBots known = " + deadBots.size() + "\n";
		for (fighterBot b : deadBots.values()) {
			str += "  bot: " + b.getName() + "\n";
		}
		str += _botsmanager.toString();
		return str;
	}
}
