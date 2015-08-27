// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class masterBotGunManager extends gunManager {

	public	masterBotGunManager() {
		gunList = new LinkedList<baseGun>();
		gunList.add( new linearGun() );
	}

	public	masterBotGunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public void manage() {
		// very simple: chose the nearest bot
		if ( myBot.getEnemyBots().size() == 0 ) {
			return;
		}
		double dist2closestBot = 1e6; // something very large
		double distNew;
		fighterBot targetBot = myBot.getEnemyBots().getFirst();
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			distNew = myBot.getPosition().distance( eBot.getPosition() );
			if ( distNew < dist2closestBot ) {
				dist2closestBot = distNew;
				targetBot = eBot;
			}
		}
		if ( targetBot != null ) {
			double bulletEnergy = 1;
			LinkedList<firingSolution> fSols = gunList.getFirst().getFiringSolutions( myBot.getInfoBot(), targetBot.getInfoBot(), myBot.getTime(), bulletEnergy );
			if ( fSols.size() >= 1) {
				firingSolution fS = fSols.getFirst();
				double firingAngle = fS.firingAngle;
				double gunAngle = myBot.proxy.getGunHeading();
				double angle = math.shortest_arc(firingAngle-gunAngle);
				myBot.proxy.setTurnGunRight(angle);
				if ( angle  <= robocode.Rules.GUN_TURN_RATE ) {
					myBot.proxy.setFireBullet(bulletEnergy);
				}
			}
		}
	}

	
}
