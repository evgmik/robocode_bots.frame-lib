// -*- java -*-

package eem.frame.wave;

import eem.frame.core.*;
import eem.frame.bot.*;
import eem.frame.gun.*;
import eem.frame.misc.*;

import robocode.Bullet;
import java.util.LinkedList;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class waveWithBullets extends wave {
	public LinkedList<firingSolution> firingSolutions = new LinkedList<firingSolution>();

	public waveWithBullets( wave w ) {
		super( w.getFiredBot(), w.getFiredTime(), w.getBulletEnergy() );
	}

	public LinkedList<firingSolution> getFiringSolutions() {
		return firingSolutions;
	}

	public void addFiringSolution( firingSolution fS ) {
		firingSolutions.add(fS);
	}

	public void removeFiringSolution( firingSolution fS ) {
		firingSolutions.remove(fS);
	}

	public double getDanger( long time, Point2D.Double dP ) {
		double waveDangerRadius = 100;
		double waveDanger= 1.0;
		double dL = 0;
		double dist = dP.distance( firedPosition ) - getDistanceTraveledAtTime( time );
		if ( dist <= Math.sqrt(2) * physics.robotHalfSize ) {
			// wave is passing through a bot at point dP
			for ( firingSolution fS : firingSolutions ) {
				dL += fS.getDanger( time, dP );
			}
		}
		return dL;
	}

	public void markFiringSolutionWhichHitBotAt( Point2D.Double botPos, String enemyName, long time ) {
		LinkedList<firingSolution> hitSolutions = this.getFiringSolutionsWhichHitBotAt( botPos,  time );
		for ( firingSolution fS : hitSolutions ) {
			if ( fS.getTargetBotName().equals(enemyName) ) {
				// this bullet is intended for this bot
				if ( fS.isActive() ) {
					updateStatsForHitBy(fS);
					fS.setActiveFlag( false );
				}
			} else {
				// FIXME: count somehow unintentional hits
			}
		}
	}

	public void updateStatsForHitBy( firingSolution fS) {
		String str = "hitFS";
		String separator = " ";

		str += separator;
		str += "target:" + fS.getTargetBotName();

		str += separator;
		str += "gun:" + fS.getGunName();

		str += separator;
		str += "distance:" + fS.getDistanceAtLastAim();

		logger.routine( str );
	}

	public LinkedList<firingSolution> getFiringSolutionsWhichHitBotAt( Point2D.Double p, long time ) {
		LinkedList<firingSolution> hitSolutions = new LinkedList<firingSolution>();

		for ( firingSolution fS : firingSolutions ) {
			if ( fS.didItHitBotAtPos( p, time ) ) {
				// fix me use proper bounding bot 
				hitSolutions.add( fS );
			}
		}
		return hitSolutions;
	}

	public void setMyWavePassedOverTargetFlag( String enemyName, boolean status) {
		for ( firingSolution fS : firingSolutions ) {
			if ( fS.getTargetBotName().equals( enemyName ) ) {
				fS.setMyWavePassedOverTargetFlag( status );
			}
		}
	}

	public int getNumOfBullets() {
		return firingSolutions.size();
	}

	public void onPaint(Graphics2D g, long time) {
		super.onPaint( g, time );
		g.setColor(waveColor);

		// draw overall  wave
		for ( firingSolution fS : firingSolutions ) {
			fS.onPaint( g, time );
		}
	}
}
