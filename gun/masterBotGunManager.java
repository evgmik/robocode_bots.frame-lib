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
		targetBot = findTheBestTarget();
		if ( targetBot != null ) {
			double bulletEnergy = bulletEnergyVsDistance( targetBot );
			if ( bulletEnergy >= (myBot.getEnergy() - 1e-4) ) {
				// do not fire or we will get ourself disabled
				return;
			}
			firingSolution fS = getTheBestFiringSolution( targetBot, bulletEnergy );
			if ( fS != null) {
				aimAndFire( fS );
			}
		}
	}

	protected double bulletEnergyVsDistance( fighterBot targetBot ) {
		double targetDistance = myBot.getPosition().distance( targetBot.getPosition() );
		double bulletEnergy = Math.min( 500/targetDistance, robocode.Rules.MAX_BULLET_POWER);
		// no point to fire bullets more energetic than enemy bot energy level
		bulletEnergy = Math.min( bulletEnergy, physics.minReqBulEnergyToKillTarget( targetBot.getEnergy() ) );

		bulletEnergy = Math.max( bulletEnergy, robocode.Rules.MIN_BULLET_POWER );

		return bulletEnergy;
	}

	public void aimAndFire( firingSolution fS ) {
		if ( fS == null) {
			return;
		}
		double bulletEnergy = fS.bulletEnergy;
		double firingAngle = fS.firingAngle;
		double gunAngle = myBot.proxy.getGunHeading();
		double angle = math.shortest_arc(firingAngle-gunAngle);
		myBot.proxy.setTurnGunRight(angle);
		if ( angle  <= robocode.Rules.GUN_TURN_RATE ) {
			bulletEnergy = Math.max( bulletEnergy, 0 ); // zero means no fire
			myBot.proxy.setFireBullet(bulletEnergy);
		}
	}

	public fighterBot findTheBestTarget() {
		fighterBot targetBot = null;
		if ( myBot.getEnemyBots().size() == 0 ) {
			return targetBot;
		}
		// very simple algorithm: chose the nearest bot
		double dist2closestBot = 1e6; // something very large
		double distNew;
		targetBot = myBot.getEnemyBots().getFirst();
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			distNew = myBot.getPosition().distance( eBot.getPosition() );
			if ( distNew < dist2closestBot ) {
				dist2closestBot = distNew;
				targetBot = eBot;
			}
		}
		return targetBot;
	}
	
	public firingSolution getTheBestFiringSolution( fighterBot targetBot, double bulletEnergy ) {
		firingSolution fS = null;
		// here we do very simple choice
		// we use the first gun form the list 
		LinkedList<firingSolution> fSols = gunList.getFirst().getFiringSolutions( myBot.getInfoBot(), targetBot.getInfoBot(), myBot.getTime(), bulletEnergy );
		// and chose first available solution 
		if ( fSols.size() >= 1) {
			fS = fSols.getFirst();
		}
		return fS;
	}
}
