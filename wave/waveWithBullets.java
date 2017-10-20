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
	protected double targetLateralSpeedSignNoZero;
	protected double headOnAngle = 0;
	protected double MEA = 0, posMEA=0, negMEA=0;
	protected Color gfColor = new Color(0xff, 0x00, 0x00, 0x80);
	protected int numGuessFactorBins = 0;
	protected double gfDangerWeight = 0.91; // 0 to 1, set high for GF flatteners or rambot avoidance
	protected double fsDangerWeight = 1.0 - gfDangerWeight;
	protected double[] gfDanger = null;
	protected double[] combGFdanger = null;
	protected double[] fsDanger = null;
	protected double[] shadows = null;
	protected double meaMarkerLenth = 10;
	protected double gfDangerMarkerScale = 300; // unity probability gives this length

	public waveWithBullets( wave w, int numGuessFactorBins ) {
		super( w.getFiredBot(), w.getFiredTime(), w.getBulletEnergy() );
		setNumberOfGuessFactors( numGuessFactorBins );
		for ( int i=0; i< numGuessFactorBins; i++ ) {
			gfDanger[i] = 1./numGuessFactorBins;
			fsDanger[i] = 0;
			shadows[i] = 0;
		}
		calcCombineDanger();
	}
	
	public void calcCombineDanger() {
		//profiler.start("calcCombineDanger");
		for ( int i=0; i< combGFdanger.length; i++ ) {
			combGFdanger[i] = 0;
			// shadow of 1 means that this GF is fully safe
			combGFdanger[i] += (1 - shadows[i]) * (
				       	gfDangerWeight * gfDanger[i]
					+ fsDangerWeight * fsDanger[i]
					);
		}
		ArrayStats  stats = new ArrayStats( combGFdanger );
		combGFdanger = stats.getProbDensity();
		//profiler.stop("calcCombineDanger");
	}

	public LinkedList<firingSolution> getFiringSolutions() {
		return firingSolutions;
	}

	public waveWithBullets setNumberOfGuessFactors( int N ) {
		numGuessFactorBins = N;
		gfDanger = new double[numGuessFactorBins];
		fsDanger = new double[numGuessFactorBins];
		shadows = new double[numGuessFactorBins];
		combGFdanger = new double[numGuessFactorBins];
		return this;
	}

	public fighterBot getTargetBot() {
		return targetBot;
	}

	public waveWithBullets setTargetBot(fighterBot tBot ) {
		targetBot = tBot;
		if ( tBot != null ) {
			botStatPoint tBStat = targetBot.getStatClosestToTime( firedTime - 1 );
			double latteralSpeed = tBStat.getLateralSpeed( firedPosition );
			targetLateralSpeedSignNoZero = math.signNoZero( latteralSpeed );
			headOnAngle = math.angle2pt( firedPosition, tBStat.getPosition() );
			MEA = physics.calculateMEA( bulletSpeed );
			posMEA = physics.calculateConstrainedMEA( bulletSpeed, firedPosition, tBStat.getPosition(), true);
			negMEA = physics.calculateConstrainedMEA( bulletSpeed, firedPosition, tBStat.getPosition(), false);
		} else {
			targetLateralSpeedSignNoZero = 1;
		}
		return this;
	}

	public void copyGFarray(double[] gfSrc ) {
		ArrayStats  stats = new ArrayStats( gfSrc );
		gfDanger = stats.getProbDensity();
		// fixme: do the flip in the caller, it is silly to do it here
		if ( targetLateralSpeedSignNoZero < 0 && !true) {
			// reflect GF array, we have some logic about symmetry
			for(int i=0; i <= numGuessFactorBins/2; i++) {
				// swap
				double tmp = gfDanger[i];
				gfDanger[i] = gfDanger[ numGuessFactorBins - 1 - i ];
				gfDanger[ numGuessFactorBins - 1 - i ] = tmp;
			}

		}
	}

	public void calcSafetyCorridorsShadowsGFpositions() {
		shadows = new double[numGuessFactorBins];
		if (targetBot == null ) {
			return;
		}
		botStatPoint tBStat = targetBot.getStatClosestToTime( firedTime - 1 );
		long time = 0; // it is currently not used, better do dist relevant to current target position
		double dist = Math.abs(tBStat.getPosition().distance( firedPosition ) );
		for(int i=0; i < numGuessFactorBins; i++) {
			double gf =  math.bin2gf( i, numGuessFactorBins);
			double a = headOnAngle + gf * MEA;
			Point2D.Double pnt = math.project( firedPosition, a, dist );
			safetyCorridor botShadow = this.getSafetyCorridor( pnt );

			double botShadowSize = botShadow.getCorridorSize();
			double corridorsCoverage = 0;
			int overlapCnt = 0;
			for ( safetyCorridor sC: safetyCorridors ) {
				safetyCorridor overlap = sC.getOverlap( botShadow );
				if ( overlap != null ) {
					corridorsCoverage += overlap.getCorridorSize();
					overlapCnt++;
				}
			}
			double eps = 2e-14;
			if ( corridorsCoverage > (botShadowSize+eps) ) {
				logger.error("error: check safety corridors addition code, looks like there were some overlapping corridors by " + (corridorsCoverage - botShadowSize)/botShadowSize);
				logger.error("error: corridors overlapping count = " + overlapCnt );
				logger.error("error: coverage size = " + corridorsCoverage );
				logger.error("error: shadow size = " + botShadowSize );
				corridorsCoverage = botShadowSize;
			}
			if ( corridorsCoverage > 0 ) {
				// shadows proportional to coverage:
				// 1 means that a bot in this position fully covered by  safety corridors
				shadows[i] = Math.min( corridorsCoverage/botShadowSize, 1);
			}
		}
	}

	public double getAngleGF( double a ) {
		return math.shortest_arc(a - headOnAngle)/MEA;
	}

	public double getPointGF( Point2D.Double p ) {
		double hitAngle    = math.angle2pt( firedPosition, p);
		return math.shortest_arc(hitAngle - headOnAngle)/MEA;
	}

	public void calcFiringSolutionGFdangers() {
		profiler.start("calcFiringSolutionGFdangers");
		fsDanger = new double[numGuessFactorBins];
		if (targetBot == null ) {
			profiler.stop("calcFiringSolutionGFdangers");
			return;
		}
		botStatPoint tBStat = targetBot.getStatClosestToTime( firedTime - 1 );
		long time = 0; // it is currently not used, better do dist relevant to current target position
		double dist = Math.abs(tBStat.getPosition().distance( firedPosition ) );
		for(int i=0; i < numGuessFactorBins; i++) {
			double gf =  math.bin2gf( i, numGuessFactorBins);
			double a = headOnAngle + gf * MEA;
			Point2D.Double pnt = math.project( firedPosition, a, dist );
			safetyCorridor botShadow = this.getSafetyCorridor( pnt );
			for ( firingSolution fS : firingSolutions ) {
				fsDanger[i] += fS.getDanger( time, botShadow );
			}
		}
		profiler.stop("calcFiringSolutionGFdangers");
	}

	public void addFiringSolutions( LinkedList<firingSolution> firingSolutions ) {
		for ( firingSolution fS: firingSolutions ) {
			this.addFiringSolution(fS);
		}
		calcFiringSolutionGFdangers();
		calcCombineDanger();
	}

	public void addFiringSolution( firingSolution fS ) {
		firingSolutions.add(fS);
	}

	public void removeFiringSolution( firingSolution fS ) {
		firingSolutions.remove(fS);
		calcFiringSolutionGFdangers();
		calcCombineDanger();
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
			// gfCorridorSum /= gfDanger.length;
			dL += gfCorridorSum;
		}
		return dL;
	}

	public double getWaveDanger( long time, Point2D.Double dP ) {
		// use danger at the closest matching GF to the point
		//profiler.start("waveWithBullets.getWaveDanger");
		double dL = 0;
		double dist = Math.abs(dP.distance( firedPosition ) - getDistanceTraveledAtTime( time ) );
		if ( dist <= physics.robotHalfDiagonal ) {
			double hitAngle = math.angle2pt( firedPosition, dP );
			double gf = getFiringGuessFactor( hitAngle );
			long i = math.gf2bin( gf, combGFdanger.length );
			// FIXME: use the full bot shadow size
			dL += combGFdanger[ (int)i];
		}
		//profiler.stop("waveWithBullets.getWaveDanger");
		return dL;
	}

	public double _getWaveDanger( long time, Point2D.Double dP ) {
		// this is essentially danger from a wave with no bullets
		// but if there are safety corridors, than danger is decreased
		double dL = 0;
		profiler.start("waveWithBullets._getWaveDanger");
		double dist = Math.abs(dP.distance( firedPosition ) - getDistanceTraveledAtTime( time ) );
		if ( dist <= physics.robotHalfDiagonal ) {
			safetyCorridor botShadow = this.getSafetyCorridor( dP );
			double shadowSize = botShadow.getCorridorSize();

			// random hit probability if enemy aims with in MEA
			//double waveDanger= shadowSize/physics.calculateMEA( bulletSpeed )/2;
			double waveDanger = 0;
			
			// This part uses provided GF danger
			// but it is time/CPU expensive.
			waveDanger += getGFDanger( time, botShadow );
			dL += waveDanger;

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
				//FIXME: be smarter about it, check those GF which are in the corridor
				dL -= waveDanger*corridorsCoverage/shadowSize;
				if ( dL <= 0 ) {
					logger.error("error: in Safety Corridors logic. Wave danger MUST NOT drop below zero");
					dL = 0;
				}
			}
		}
		profiler.stop("waveWithBullets._getWaveDanger");
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
				dL += 1*fS.getDanger( time, botShadow );
			}
		}
		profiler.stop("waveWithBullets.getFiringSolutionsDanger");
		return dL;
	}

	public double getDanger( long time, Point2D.Double dP ) {
		//profiler.start("waveWithBullets.getDanger");
		double waveDangerRadius = 100;
		double dL = 0;
		double dist = Math.abs(dP.distance( firedPosition ) - getDistanceTraveledAtTime( time ) );
		dL += getWaveDanger( time, dP );
		//profiler.stop("waveWithBullets.getDanger");
		return dL*10;
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
		calcFiringSolutionGFdangers();
		calcCombineDanger();
	}

	public void removeBulletsOutsideOfHitRegion(long time) {
		//profiler.start("wB_removeBulletsOutsideOfHitRegion");
		LinkedList<firingSolution> fStoRemove = new LinkedList<firingSolution>();
		if ( time > firedTime+5 && firingSolutions.size() > 0) { 
			// if wave did not travel enough bot shadow > 180 degrees
			safetyCorridor hC = getHitCoridor( time );
			fighterBot fBot = targetBot.getGameInfo().getFighterBot( firedBot.getName() );
			gunManager gm = fBot.getGunManager();
			for ( firingSolution fS : firingSolutions ) {
				if ( !fS.isItInCoridor( hC ) ) {
					gm.logHitOrMissForMyFS(fS);

					fStoRemove.add(fS);
				}
			}
			if ( fStoRemove.size() > 0 ) {
				firingSolutions.removeAll(fStoRemove);
				if ( !fBot.isItMasterBotDriver() ) {
					calcFiringSolutionGFdangers();
					calcCombineDanger();
				}
			}
		}
		//profiler.stop("wB_removeBulletsOutsideOfHitRegion");
	}

	public void addSafetyCorridor( fighterBot bot) {
		safetyCorridor sC = getSafetyCorridor( bot );
		addSafetyCorridor ( sC );
	}

	public void addSafetyCorridor( firingSolution fS) {
		safetyCorridor sC = getSafetyCorridor( fS );
		addSafetyCorridor ( sC );
	}

	public void addSafetyCorridor( safetyCorridor sC ) {
		if ( sC != null ) {
			removeFiringSolutionsInSafetyCorridor( sC );
			addToSafetyCorridors(sC);
			calcSafetyCorridorsShadowsGFpositions();
			calcCombineDanger();
		}
	}

	public void drawGFArrayDanger(Graphics2D g, long time, double[] bins, Color binsColor) {
		if ( bins == null ) {
			return;
		}
		if ( targetBot == null ) {
			return;
		}
		g.setColor(binsColor);
		int Nbins = bins.length;
		Point2D.Double prevP = null;
		double prevPointDanger=0;
		// show danger probability distribution
		for ( int i=0; i< Nbins; i++ ) {
			double gf =  math.bin2gf( i, Nbins);
			double dL = bins[i];
			double a = headOnAngle + gf * MEA;
			double dist = (time - firedTime) * bulletSpeed;

			Point2D.Double strtP = math.project( firedPosition, a, dist );
			Point2D.Double endP;
			// show MEA range
			if ( i==0 || i==(Nbins-1)
			     || i==math.gf2bin(posMEA/MEA, Nbins)
			     || i==math.gf2bin(negMEA/MEA, Nbins)
			) {
				math.project( firedPosition, a, dist );
				endP = math.project( firedPosition, a, dist - meaMarkerLenth );
				graphics.drawLine( g, strtP,  endP );
			}
			// show GF danger
			double gfDangerMarkerLength = dL*gfDangerMarkerScale;
			endP = math.project( firedPosition, a, dist + gfDangerMarkerLength );
			graphics.drawLine( g, strtP,  endP );
			if ( prevP != null ) {
				// this plot envelope of GF dangers
				graphics.drawLine( g, prevP,  endP );
			}
			prevP = endP;
		}
	}

	public double getEscapeAngleAtTime(long time){
		Point2D.Double tPos = targetBot.getPositionClosestToTime(time);
		double timeToReach = getTimeToReach( tPos );
		double dt = timeToReach + firedTime - time + ( physics.robotHalfDiagonal / bulletSpeed) ;
		if ( dt <= 0 )
			return 0;
		// not sure that this is precise enough but it gives proper border cases
		return Math.toDegrees( Math.asin( dt * robocode.Rules.MAX_VELOCITY / firedPosition.distance(tPos) ) );
	}

	public void drawTotalWaveDanger(Graphics2D g, long time,  Color binsColor) {
		if ( targetBot == null ) {
			return;
		}
		int Nbins = gfDanger.length;
		double[] bins = new double[Nbins];
		for ( int i=0; i< Nbins; i++ ) {
			double gf =  math.bin2gf( i, Nbins);
			double dL = bins[i];
			double a = headOnAngle + gf * MEA;
			double dist = (time - firedTime) * bulletSpeed;
			Point2D.Double strtP = math.project( firedPosition, a, dist );
			bins[i] = getDanger( time, strtP );
		}
		bins = combGFdanger;
		ArrayStats  stats = new ArrayStats( bins );
		bins = stats.getProbDensity();
		Color waveDangerColor = new Color(0xff, 0xff, 0x00, 0x80);
		drawGFArrayDanger(g, time, bins, waveDangerColor);
	}

	public safetyCorridor getHitCoridor( long time ) {
		//profiler.start("getHitCoridor");
		// angles range within which target can be at given time
		// at fire time it is +/- MEA
		// but as time progress the hit area decreases
		// it is handy to see which bullets might still hit the target
		double minA=0, maxA=0;
		if ( targetBot != null ) {
			double distTraveled = getDistanceTraveledAtTime(time);
			double botHalfWidthAngle = Math.toDegrees( Math.atan(physics.robotHalfDiagonal/distTraveled) );
			Point2D.Double tgtPosNow = targetBot.getPositionClosestToTime(time);
			double hitAngle    = math.angle2pt( firedPosition, tgtPosNow);
			double escapeAngle = getEscapeAngleAtTime( time );
			minA = hitAngle - botHalfWidthAngle - escapeAngle;
			maxA = hitAngle + botHalfWidthAngle + escapeAngle;
		}
		//profiler.stop("getHitCoridor");
		return new safetyCorridor( minA, maxA );
	}

	public void drawCurrentEscapeAngle(Graphics2D g, long time, Color c) {
		safetyCorridor hC = getHitCoridor( time );
		double minA = hC.getMinAngle();
		double maxA = hC.getMaxAngle();
		double distTraveled = getDistanceTraveledAtTime(time);
		g.setColor( c );
		graphics.drawCircArc( g, firedPosition, distTraveled, minA, maxA );
		graphics.drawCircArc( g, firedPosition, distTraveled-1, minA, maxA );
		graphics.drawCircArc( g, firedPosition, distTraveled-2, minA, maxA );
		graphics.drawCircArc( g, firedPosition, distTraveled-3, minA, maxA );
		graphics.drawCircArc( g, firedPosition, distTraveled-4, minA, maxA );
		graphics.drawCircArc( g, firedPosition, distTraveled-5, minA, maxA );
	}

	public void drawGFdanger(Graphics2D g, long time) {
		fighterBot fBot = targetBot.getGameInfo().getFighterBot( firedBot.getName() );
		if ( !fBot.isItMasterBotDriver() ) {
			drawGFArrayDanger(g, time, gfDanger, gfColor);
			drawTotalWaveDanger( g, time,  gfColor) ;
			drawCurrentEscapeAngle( g, time,  gfColor) ;
		}
	}

	public void onPaint(Graphics2D g, long time) {
		super.onPaint( g, time );
		g.setColor(waveColor);

		drawGFdanger(g, time);

		// draw overall  wave
		for ( firingSolution fS : firingSolutions ) {
			fS.onPaint( g, time );
		}

		for ( safetyCorridor sC : safetyCorridors ) {
			drawSafetyCorridor(g, sC, time);
		}
		// plot Head on Line
		g.setColor( new Color(0x00, 0x00, 0x00, 0x80) );
		Point2D.Double endP = math.project( firedPosition, headOnAngle, getDistanceTraveledAtTime(time) );
		Point2D.Double strtP = math.project( firedPosition, headOnAngle, getDistanceTraveledAtTime( Math.max( 0, time-2) ) );
		graphics.drawLine( g, strtP,  endP );
	}
}
