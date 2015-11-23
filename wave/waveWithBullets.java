// -*- java -*-

package eem.frame.wave;

import eem.frame.core.*;
import eem.frame.bot.*;
import eem.frame.gun.*;
import eem.frame.misc.*;

import robocode.Bullet;
import java.util.LinkedList;
import java.awt.Graphics2D;
import java.awt.Color;
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

	public double getWaveDanger( long time, Point2D.Double dP ) {
		// this is essentially danger from a wave with no bullets
		// but if there are safety corridors, than danger is decreased
		double dL = 0;
		profiler.start("getWaveDanger");
		double dist = dP.distance( firedPosition ) - getDistanceTraveledAtTime( time );
		if ( dist <= physics.robotHalfDiagonal ) {
			safetyCorridor botShadow = this.getSafetyCorridor( dP );
			double shadowSize = botShadow.getCorridorSize();

			// random hit probability if enemy aims with in MEA
			double waveDanger= shadowSize/physics.calculateMEA( bulletSpeed )/2;

			double corridorsCoverage = 0;
			int overlapCnt = 0;
			for ( safetyCorridor sC: safetyCorridors ) {
				safetyCorridor overlap = sC.getOverlap( botShadow );
				if ( overlap != null ) {
					corridorsCoverage += overlap.getCorridorSize();
					overlapCnt++;
					if ( overlap.getCorridorSize() >shadowSize ) {
						logger.dbg( "--------------");
						logger.dbg( sC.toString() );
						logger.dbg( botShadow.toString() );
					}
				}
			}
			double eps = 0;
			if ( corridorsCoverage > (shadowSize+eps) ) {
				logger.error("error: check safety corridors addition code, looks like there we some overlapping corridors by " + (corridorsCoverage - shadowSize)/shadowSize);
				logger.error("error: corridors overlapping count = " + overlapCnt );
				logger.error("error: coverage size = " + corridorsCoverage );
				logger.error("error: shadow size = " + shadowSize );
				corridorsCoverage = shadowSize;
			}
			if ( corridorsCoverage >= 0 ) {
				dL += waveDanger*( 1 - corridorsCoverage/shadowSize );
				if ( dL <= 0 ) {
					dL = 0;
				}
			}
		}
		profiler.stop("getWaveDanger");
		return dL;
	}

	public double getDanger( long time, Point2D.Double dP ) {
		double waveDangerRadius = 100;
		double dL = 0;
		double dist = dP.distance( firedPosition ) - getDistanceTraveledAtTime( time );
		if ( dist <= physics.robotHalfDiagonal ) {
			dL += getWaveDanger( time, dP );
			if ( dL == 0 ) {
				// bot is fully covered by safety corridors
				return 0;
			}

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
					//updateStatsForHitBy(fS);
					fS.setActiveFlag( false );
				}
			} else {
				// FIXME: count somehow unintentional hits
			}
		}
	}

	public firingSolution getRealFiringSolution() {
		firingSolution realBullet = null;	
		for ( firingSolution fS : firingSolutions ) {
			if ( fS.isRealBullet() ) {
				realBullet = fS;
				break;
			}
		}
		return realBullet;
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

	public void removeFiringSolutionsInSafetyCorridor( safetyCorridor sC ) {
		LinkedList<firingSolution> fStoRemove = new LinkedList<firingSolution>();
		for ( firingSolution fS : firingSolutions ) {
			if ( fS.isItInCoridor( sC ) ) {
				fStoRemove.add(fS);
			}
		}
		firingSolutions.removeAll(fStoRemove);
	}

	public void addSafetyCorridor( fighterBot bot) {
		safetyCorridor sC = getSafetyCorridor( bot );
		if ( sC != null ) {
			removeFiringSolutionsInSafetyCorridor( sC );
			addToSafetyCorridors(sC);
		}
	}

	public void addSafetyCorridor( firingSolution fS) {
		safetyCorridor sC = getSafetyCorridor( fS );
		if ( sC != null ) {
			removeFiringSolutionsInSafetyCorridor( sC );
			addToSafetyCorridors(sC);
		}
	}

	public void onPaint(Graphics2D g, long time) {
		super.onPaint( g, time );
		g.setColor(waveColor);

		// draw overall  wave
		for ( firingSolution fS : firingSolutions ) {
			fS.onPaint( g, time );
		}

		for ( safetyCorridor sC : safetyCorridors ) {
			drawSafetyCorridor(g, sC, time);
		}
	}
}
