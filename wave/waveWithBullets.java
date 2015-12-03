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
	protected fighterBot targetBot = null;
	protected Color gfColor = new Color(0xff, 0x00, 0x00, 0x80);
	protected int numGuessFactorBins = 31;
	protected double[] gfDanger = new double[numGuessFactorBins];

	public waveWithBullets( wave w ) {
		super( w.getFiredBot(), w.getFiredTime(), w.getBulletEnergy() );
		for ( int i=0; i< numGuessFactorBins; i++ ) {
			gfDanger[i] = 1;
		}
	}

	public LinkedList<firingSolution> getFiringSolutions() {
		return firingSolutions;
	}

	public void setTargetBot(fighterBot tBot ) {
		targetBot = tBot;
	}

	public void copyGFarray(double[] gfSrc ) {
		numGuessFactorBins = gfSrc.length;
		gfDanger = new double[numGuessFactorBins];
		for ( int i=0; i< numGuessFactorBins; i++ ) {
			gfDanger[i] = gfSrc[i];
		}
		gfDanger = math.normArray( gfDanger );
	}

	public void addFiringSolution( firingSolution fS ) {
		firingSolutions.add(fS);
	}

	public void removeFiringSolution( firingSolution fS ) {
		firingSolutions.remove(fS);
	}

	public double getFiringGuessFactor( double absFiringAngle ) {
		double gf = 0;
		if ( targetBot != null ) {
			gf = getFiringGuessFactor( targetBot.getInfoBot(), absFiringAngle );
		}
		return gf;
	}

	public double getGFDanger( long time, safetyCorridor botShadow ) {
		double dL =0;
		// Let's calculate the danger due to GF stats
		double gfStrt = getFiringGuessFactor( botShadow.getMinAngle() );
		double gfEnd  = getFiringGuessFactor( botShadow.getMaxAngle() );
		if ( ((gfStrt < -1) || (gfStrt > 1)) && ((gfEnd < -1) || (gfEnd > 1)) ) {
			// both edges are outside of MEA
			// most likely we called this function to help
			// danger map drawing
		} else {
			long iStrt = math.gf2bin( gfStrt, gfDanger.length );
			long iEnd  = math.gf2bin( gfEnd,  gfDanger.length );

			if ( iStrt > iEnd ) {
				// swap them
				long tmp = iStrt;
				iStrt = iEnd;
				iEnd =tmp;
			}
			double gfCorridorSum = 0;
			int cnt = 0;
			for ( long i = iStrt; i <= iEnd; i++ ) {
				gfCorridorSum += gfDanger[ (int)i];
				cnt++;
			}
			// now we normalize it
			gfCorridorSum /= gfDanger.length;
			dL += gfCorridorSum;
		}
		return dL;
	}

	public double getWaveDanger( long time, Point2D.Double dP ) {
		// this is essentially danger from a wave with no bullets
		// but if there are safety corridors, than danger is decreased
		double dL = 0;
		profiler.start("waveWithBullets.getWaveDanger");
		double dist = Math.abs(dP.distance( firedPosition ) - getDistanceTraveledAtTime( time ) );
		if ( dist <= physics.robotHalfDiagonal ) {
			safetyCorridor botShadow = this.getSafetyCorridor( dP );
			double shadowSize = botShadow.getCorridorSize();

			// random hit probability if enemy aims with in MEA
			double waveDanger= shadowSize/physics.calculateMEA( bulletSpeed )/2;
			dL = waveDanger;

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
				dL -= waveDanger*corridorsCoverage/shadowSize;
				if ( dL <= 0 ) {
					dL = 0;
				}
			}
			if ( dL > 0 ) {
				// The bot/point is not completely covered by safety corridors
				// This part helps with flattening of the bot GF
				// but it is time/CPU expensive.
				//
				// TODO: for now I disable it
				//dL += getGFDanger( time, botShadow );
			}
		}
		profiler.stop("waveWithBullets.getWaveDanger");
		return dL;
	}

	public double getFiringSolutionsDanger( long time, Point2D.Double dP ) {
		profiler.start("waveWithBullets.getFiringSolutionsDanger");
		double dL = 0;
		double dist = Math.abs(dP.distance( firedPosition ) - getDistanceTraveledAtTime( time ) );
		if ( dist <= physics.robotHalfDiagonal ) {
			safetyCorridor botShadow = this.getSafetyCorridor( dP );
			// wave is passing through a bot at point dP
			for ( firingSolution fS : firingSolutions ) {
				//dL += fS.getDanger( time, dP );
				dL += fS.getDanger( time, botShadow );
			}
		}
		profiler.stop("waveWithBullets.getFiringSolutionsDanger");
		return dL;
	}

	public double getDanger( long time, Point2D.Double dP ) {
		profiler.start("waveWithBullets.getDanger");
		double waveDangerRadius = 100;
		double dL = 0;
		double dist = Math.abs(dP.distance( firedPosition ) - getDistanceTraveledAtTime( time ) );
		if ( dist <= physics.robotHalfDiagonal ) {
			dL += getWaveDanger( time, dP );
			if ( dL == 0 ) {
				// bot is fully covered by safety corridors
			} else {
				dL += getFiringSolutionsDanger( time, dP );
			}
		}
		profiler.stop("waveWithBullets.getDanger");
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

	public void drawGFdanger(Graphics2D g, long time) {
		if ( targetBot == null ) {
			return;
		}
		g.setColor(gfColor);
		double MEA = physics.calculateMEA( bulletSpeed );
		botStatPoint tBStat = targetBot.getStatClosestToTime( firedTime - 1 );
		double latteralSpeed = tBStat.getLateralSpeed( firedPosition );
		double headOnAngle = math.angle2pt( firedPosition, tBStat.getPosition() );
		Point2D.Double prevP = null;
		for ( int i=0; i< numGuessFactorBins; i++ ) {
			double gf =  math.bin2gf( i, numGuessFactorBins) * math.signNoZero( latteralSpeed );
			double dL = gfDanger[i];
			double a = headOnAngle + gf * MEA;
			double dist = (time - firedTime) * bulletSpeed;

			Point2D.Double strtP = math.project( firedPosition, a, dist );
			dist += dL*10;
			Point2D.Double endP = math.project( firedPosition, a, dist );
			graphics.drawLine( g, strtP,  endP );
			if ( prevP != null ) {
				// this plot envelope of GF dangers
				graphics.drawLine( g, prevP,  endP );
			}
			prevP = endP;

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

		drawGFdanger(g, time);
	}
}
