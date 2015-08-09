// -*- java -*-

package eem.bot;

import eem.core.*;
import eem.bot.*;
import eem.wave.*;
import eem.gameInfo.*;
import eem.misc.*;

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

	public static HashMap<String,InfoBot> liveBots     = new HashMap<String, InfoBot>();
	public static HashMap<String,InfoBot> deadBots = new HashMap<String, InfoBot>();;
	protected double distAtWhichHitProbabilityDrops = 200.0; // phenomenological parameter

	public botsManager(CoreBot bot) {
		myBot = bot;
		updateMasterBotStatus(bot);
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
		InfoBot dBot = liveBots.get(botName);
		deadBots.put( botName, dBot);
		liveBots.remove( botName );
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
		iBot.update( new botStatPoint(myBot) );
		liveBots.put(botName, iBot);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		String botName = e.getName();
		InfoBot iBot = liveBots.get(botName);
		if ( iBot == null ) {
		       	// this is newly discovered bot
			iBot = new InfoBot(botName);
		}
		iBot.update( new botStatPoint(myBot, e) );
		liveBots.put(botName, iBot);
		double eDrop = iBot.energyDrop();
		if ( eDrop > 0 ) {
			// wave/bullet is fired
			// FIXME: be smarter about it: check collisions and bullets hits
			// enemy energy drop detected by one tic later thus -1
			wave w = new wave( iBot, myBot.getTime()-1, eDrop );
			myBot._gameinfo._wavesManager.add( w );
		}
	}

	public void onPaint(Graphics2D g) {
		for (InfoBot bot : liveBots.values()) 
		{
			bot.onPaint(g);
		}
	}
}
