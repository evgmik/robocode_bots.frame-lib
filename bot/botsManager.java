// -*- java -*-

package eem.frame.bot;

import eem.frame.core.*;
import eem.frame.event.*;
import eem.frame.bot.*;
import eem.frame.wave.*;
import eem.frame.gameInfo.*;
import eem.frame.misc.*;

import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.LinkedList;

import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

public class  botsManager {
	public CoreBot myBot;
	public gameInfo _gameinfo;

	public static HashMap<String,InfoBot> liveBots = new HashMap<String, InfoBot>();
	public static HashMap<String,InfoBot> deadBots = new HashMap<String, InfoBot>();;
	protected double distAtWhichHitProbabilityDrops = 200.0; // phenomenological parameter

	public LinkedList<botListener> botListeners = new LinkedList<botListener>();

	public botsManager(CoreBot bot, gameInfo gInfo) {
		myBot = bot;
		_gameinfo = gInfo;
		//updateMasterBotStatus(bot);
		// move deadBots to alive bots, should happen at the beginning of the round
		if ( deadBots.size() >= 1) {
			for (InfoBot dBot : deadBots.values() ) {
				String botName = dBot.getName();
				liveBots.put( botName, dBot);
			}
		}
		deadBots.clear();
	}


	public InfoBot getBotByName(String botName) {
		InfoBot b = null;
		b = liveBots.get( botName );
		if ( null != b )
			return b;
		b = deadBots.get( botName );
		if ( null != b )
			return b;
		// we should never reach here
		logger.error("Bots manager cannot find bot: " + botName );
		return b;
	}

	public void initTic(long ticTime) {
		updateMasterBotStatus(myBot);
		profiler.start( "botsManager.initTic" );
		for (InfoBot bot : liveBots.values()) 
		{
			bot.initTic(ticTime);
		}
		profiler.stop( "botsManager.initTic" );
	}


	public LinkedList<InfoBot> listOfKnownBots() {
		LinkedList<InfoBot> l = new LinkedList<InfoBot>();
		l.addAll( listOfAliveBots() );
		l.addAll( listOfDeadBots() );
		return l;
	}

	public LinkedList<InfoBot> listOfAliveBots() {
		LinkedList<InfoBot> l = new LinkedList<InfoBot>();
		for (InfoBot bot : liveBots.values()) {
			l.add(bot);
		}
		return l;
	}

	public LinkedList<InfoBot> listOfDeadBots() {
		LinkedList<InfoBot> l = new LinkedList<InfoBot>();
		for (InfoBot bot : deadBots.values()) {
			l.add(bot);
		}
		return l;
	}

	public void onRobotDeath(RobotDeathEvent e) {
		String botName = e.getName();
		logger.noise( "botManager: bot " + botName + " is dead" );
		InfoBot dBot = liveBots.get(botName);
		deadBots.put( botName, dBot);
		liveBots.remove( botName );

		callListenersOnRobotDeath( dBot );
	}

	public void add(InfoBot bot) {
		liveBots.put( bot.getName(), bot );
	}

	public void onHitByBullet(HitByBulletEvent e) {
	}

	public void updateMasterBotStatus(CoreBot myBot) {
		String botName = myBot.getName();
		InfoBot iBot = liveBots.get(botName);
		if ( iBot == null ) {
		       	// this is newly discovered bot
			iBot = new InfoBot(botName);
		}
		botStatPoint bStatLast = iBot.getLast();
		botStatPoint bStatNow  = new botStatPoint( myBot );
		long t = 0;
		if ( bStatLast != null ) {
			double speedLast = Math.abs( bStatLast.getSpeed() );
			double speedNow  = Math.abs( bStatNow.getSpeed() );
			if ( Utils.isNear(  speedLast, speedNow ) ) {
				t = bStatLast.getTimeSinceVelocityChange();
				t++;
			}
		}
		bStatNow.setTimeSinceVelocityChange( t );
		iBot.update( bStatNow );
		liveBots.put(botName, iBot);
		_gameinfo.specialOnScannedRobot(iBot);
		callListenersOnScannedRobot( iBot );
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		String botName = e.getName();
		InfoBot iBot;
		// check if this is former dead bot
		iBot = deadBots.get(botName);
		if ( iBot != null ) {
			logger.noise("bot manager: ressurecting " + botName );
			logger.noise("old ref " + deadBots.get( botName) );
			logger.noise("new ref " +  iBot );
			liveBots.put( botName, iBot );
			deadBots.remove( botName);
		}
		iBot = liveBots.get(botName);
		if ( iBot == null ) {
		       	// this is newly discovered bot
			iBot = new InfoBot(botName);
		}
		botStatPoint bStatLast = iBot.getLast();
		botStatPoint bStatNow  = new botStatPoint( myBot, e );
		long t = 0;
		if ( bStatLast != null ) {
			double speedLast = Math.abs( bStatLast.getSpeed() );
			double speedNow  = Math.abs( bStatNow.getSpeed() );
			if ( Utils.isNear(  speedLast, speedNow ) ) {
				t = bStatLast.getTimeSinceVelocityChange();
				t++;
			}
		}
		bStatNow.setTimeSinceVelocityChange( t );
		iBot.update( bStatNow );
		_gameinfo.specialOnScannedRobot(iBot);
		liveBots.put(botName, iBot);
		callListenersOnScannedRobot( iBot );
	}

	public void addBotListener(botListener l) {
		botListeners.add(l);
	}

	public void callListenersOnScannedRobot(InfoBot b) {
		for ( botListener l : botListeners ) {
			l.onScannedRobot(b);
		}
	}

	public void callListenersOnRobotDeath(InfoBot b) {
		for ( botListener l : botListeners ) {
			l.onRobotDeath(b);
		}
	}

	public String toString() {
		String str = "";
		str += "botManager stats\n";
		str += " liveBots known = " + liveBots.size() + "\n";
		for (InfoBot b : liveBots.values()) {
			str += "  bot: " + b.getName() + "\n";
		}
		str += " deadBots known = " + deadBots.size() + "\n";
		for (InfoBot b : deadBots.values()) {
			str += "  bot: " + b.getName() + "\n";
		}
		return str;
	}

	public void onPaint(Graphics2D g) {
		for (InfoBot bot : liveBots.values()) 
		{
			bot.onPaint(g);
		}
	}
}
