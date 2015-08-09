// -*- java -*-
// (C) 2013 by Eugeniy Mikhailov, <evgmik@gmail.com>

package eem.radar;

import eem.core.*;
import eem.misc.*;

import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

public class radar {
	protected CoreBot myBot;

	protected int radarSpinDirection =1;
	protected static double radarMaxRotationAngle;
	protected LinkedList<String> scannedBots = new LinkedList<String>();
	protected String botToSearchFor = "";
	boolean needToTrackTarget = false;

	public radar(CoreBot bot) {
		myBot = bot;
		radarMaxRotationAngle = myBot.game_rules.RADAR_TURN_RATE ;
		needToTrackTarget = false;
		botToSearchFor = "";
	}

	public void initTic() {
		myBot.setAdjustRadarForGunTurn(true); // decouple gun and radar
	}

	public void setNeedToTrackTarget( boolean flag ) {
		needToTrackTarget = flag;
	}

	public void manage() {
		double angle = 0;
		if ( myBot.numEnemyBotsAlive == 0) {
			// we already won, no need to do anything
			return;
		}

		if ( scannedBots.size() < myBot.numEnemyBotsAlive ) {
			// this should be done only once at the begining of the round
			// we have not seen all bots thus we need to do/keep sweeping
			// performing initial sweep
			angle = radarSpinDirection*radarMaxRotationAngle;
			setTurnRadarRight(angle);
			return;
		}

		if ( needToTrackTarget ) {
			//if ( myBot._trgt.haveTarget ) {
			if ( false ) { //FIXME disabled logic for now, implement above _trgt.haveTarget
				//String bName = myBot._trgt.getName();
				//moveRadarToBot( bName );
			} else {
				refreshBotsPositions();
			}
			return;
		}

		// if nothing of above
		refreshBotsPositions();
	}

	public void refreshBotsPositions() {
		logger.noise("Refreshing bots positons");
		String bName = scannedBots.getFirst();
		moveRadarToBot( bName );
	}

	public void moveRadarToBot( String bName ) {
		logger.noise("Spinning radar to bot"+bName);
		double angle = 0;
		long lastSeenDelay = myBot.ticTime -  myBot._gameinfo._botsmanager.getBotByName( bName ).getLastSeenTime();
		if ( botToSearchFor.equals( bName ) && (lastSeenDelay > 1) ) {
			// we already set radar motion parameters
			angle = radarSpinDirection*radarMaxRotationAngle;
		} else {
			// new bot to search or we just saw this bot so its position
			// can be used for radar spin calculations
			botToSearchFor = bName;
			double radar_heading = myBot.getRadarHeading();
			double angleToLastBotPosition = math.angle2pt(myBot.myCoord, myBot._gameinfo._botsmanager.getBotByName( bName ).getPosition() );
			angle= angleToLastBotPosition - radar_heading;
			angle = math.shortest_arc(angle);
			radarSpinDirection = math.signNoZero(angle);
			angle = Math.abs( angle );
				angle+=radarMaxRotationAngle/2; // we want to overshoot
			angle = radarSpinDirection*angle;
		}
		setTurnRadarRight(angle);
	}

	protected void setTurnRadarRight(double angle) {
		double angle2rotate = math.putWithinRange(angle, -radarMaxRotationAngle, radarMaxRotationAngle);
		logger.noise("Radar rotation angle = " + angle2rotate);
		myBot.setTurnRadarRight(angle2rotate);
	}

	public void onRobotDeath(RobotDeathEvent e) {
		String dBotName = e.getName();
		scannedBots.remove( dBotName );
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		String scannedBotName = e.getName();
		for ( String bName : scannedBots ) {
			if ( bName.equals( scannedBotName ) ) {
				scannedBots.remove( bName );
				break;
			}
		}
		scannedBots.addLast( scannedBotName );
	}
}

