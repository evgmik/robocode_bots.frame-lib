// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
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

	public HashMap<String,Integer> hitByOther = new HashMap<String, Integer>();
	public HashMap<String,Integer> hitByMe = new HashMap<String, Integer>();


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
	
	//helper method to increment count in HashMap
	public void incrHashCounter( HashMap<String,Integer> map, String key ) {
		if ( map.containsKey( key ) ) {
			Integer cnt = map.get( key );
			cnt++;
			map.put( key, cnt );
		} else {
			map.put( key, 1 );
		}
	}
	
	public void incrHitCounts( String trgtBotName, String fireBotName ) {
		if ( myBot.getName().equals( trgtBotName ) ) {
			// this bot is hit by other
			incrHashCounter( hitByOther, fireBotName);
		}
		if ( myBot.getName().equals( fireBotName ) ) {
			// this bot hit someone
			incrHashCounter( hitByMe, trgtBotName);
		}
	}

	public void reportHitByOther(){
		logger.routine("Hit me count by the following bot(s)");
		for ( String bName: hitByOther.keySet() ) {
			logger.routine( " " + bName + ": " + hitByOther.get( bName ) );
		}
		if ( hitByOther.size() == 0 ) {
			logger.routine( " none to report" );
		}
	}

	public void reportHitByMe(){
		logger.routine("hit rate for the following bot(s)");
		for ( String bName: hitByMe.keySet() ) {
			logger.routine( " " + bName + ": " + logger.hitRateFormat( hitByMe.get( bName ), firedCount) );
		}
		if ( hitByMe.size() == 0 ) {
			logger.routine( " did not hit anyone: " + logger.hitRateFormat( 0, firedCount) );
		}
	}

	public void reportStats() {
		if ( myBot.isItMasterBotDriver() ) {
			reportHitByOther();
		}
		reportHitByMe();
	}

	public void incrFiredCount() {
		firedCount++;
	}


	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
		if ( targetBot != null ) {
			double R = 50;
			graphics.drawCircle( g, targetBot.getPosition(), R );
		}
	}
}
