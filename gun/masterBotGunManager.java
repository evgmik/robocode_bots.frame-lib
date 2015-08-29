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
		gunList.add( new headOnGun() );
	}

	public	masterBotGunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public void manage() {
		if ( myBot.getTime() == fireAtTime &&  myBot.proxy.getGunTurnRemaining() == 0) {
			// see firing pitfalls
			// http://robowiki.net/wiki/Robocode/Game_Physics#Firing_Pitfall
			// essentially we need to set firing solution at previous tick
			myBot.proxy.setFireBullet(finalFiringSolution.bulletEnergy);
		}
		targetBot = findTheBestTarget();
		if ( targetBot != null ) {
			double bulletEnergy = bulletEnergyVsDistance( targetBot );
			if ( bulletEnergy >= (myBot.getEnergy() - 1e-4) ) {
				// do not fire or we will get ourself disabled
				return;
			}
			bulletEnergy = Math.max( bulletEnergy, 0 ); // zero means no fire
			if ( bulletEnergy <= 0 ) {
				return; // bad bullet
			}
			firingSolution fS = getTheBestFiringSolution( targetBot, bulletEnergy );
			if ( fS == null) {
				logger.noise("time " + myBot.getTime() + " Veto on fire: no firing solution");
				return; // no solution
			}
			aimAndSetGun( fS );
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

	public void aimAndSetGun( firingSolution fS ) {
		if ( fS == null) {
			finalFiringSolution = null;
			return;
		}
		if ( fS.getQualityOfSolution() < firingSolutionQualityThreshold ) {
			logger.noise("time " + myBot.getTime() + " Veto on fire: no good enough solution");
			return; // no good enough solution
		}
		logger.noise("time " + myBot.getTime() + " firing solution is good");

		double bulletEnergy = fS.bulletEnergy;
		double firingAngle = fS.firingAngle;
		double gunAngle = myBot.proxy.getGunHeading();
		double angle = math.shortest_arc(firingAngle-gunAngle);
		myBot.proxy.setTurnGunRight(angle);
		fireAtTime = myBot.getTime() + 1;
		finalFiringSolution = fS;
		// now we need to be smart robocode engine first fires than rotate
		// the gun see
		// http://robowiki.net/wiki/Robocode/Game_Physics#Firing_Pitfall
		// so we need to fire only if required gun rotation smaller than
		// target arc.
		// So we do not fire now, instead we will check at next click
		// that gun did rotate enough
	}

	public fighterBot findTheBestTarget() {
		double distThreshold=100; // we keep old target if candidate is not closer than this
		// above should help with continues gun shift
		// when to bots are at about same distance

		long infoDelayTimeThreshold = (long) (360/robocode.Rules.RADAR_TURN_RATE + 1);
		if ( myBot.getEnemyBots().size() == 0 ) {
			return null;
		}
		// very simple algorithm: chose the nearest bot
		double dist2closestBot = 1e6; // something very large
		if  (targetBot != null) {
		       if ( (myBot.getTime() - targetBot.getLastSeenTime()) < infoDelayTimeThreshold ) {
			// let's keep old target bot as reference point
			dist2closestBot = myBot.getPosition().distance( targetBot.getPosition() );
		       }
		}
		double distNew;
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			distNew = myBot.getPosition().distance( eBot.getPosition() );
			if ( ((distNew + distThreshold) < dist2closestBot) && ((myBot.getTime() - eBot.getLastSeenTime()) < infoDelayTimeThreshold) ) {
				dist2closestBot = distNew;
				targetBot = eBot;
				logger.dbg(" dist to " + eBot.getName() + " " + distNew);
			}
		}
		return targetBot;
	}
	
	public firingSolution getTheBestFiringSolution( fighterBot targetBot, double bulletEnergy ) {
		firingSolution fS = null;
		// try each gun and chose solution with best quality
		double bestQSol = -1000;
		for ( baseGun g : gunList ) {
			LinkedList<firingSolution> fSols = g.getFiringSolutions( myBot.getInfoBot(), targetBot.getInfoBot(), myBot.getTime(), bulletEnergy );
			for ( firingSolution curFS : fSols ) {
				if ( curFS.getQualityOfSolution() > bestQSol ) {
					fS = curFS;
					bestQSol = curFS.getQualityOfSolution();
				}
			}
		}
		return fS;
	}
}
