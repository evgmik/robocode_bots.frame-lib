// -*- java -*-

package eem.dangermap;

import eem.dangermap.*;
import eem.misc.*;
import eem.wave.*;
import eem.bot.*;

import java.util.LinkedList;
import java.util.Collections;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class dangerMap {
	public LinkedList<dangerPoint> dangerPoints;
	public long ticTime=0;
	public fighterBot myBot;

	public dangerMap(fighterBot myBot) {
		dangerPoints = new LinkedList<dangerPoint>();
		this.myBot = myBot;
	}

	public void add( dangerPoint p ) {
		dangerPoints.add( p );
	}

	public void add( Point2D.Double p ) {
		double dangerLevel = 0;
		dangerPoints.add( new dangerPoint(p, dangerLevel) );
	}

	public void reCalculateDangerMap(long time) {
		ticTime = time;
		double dL;
		for( dangerPoint dP: dangerPoints ) {
			dL = calculateDangerForPoint( time, dP );
			dP.setDanger( dL );
		}
	}

	public double calculateDangerForPoint( long time, dangerPoint dP ) {
		double dL = 0;
		dL += calculateDangerFromWall(time, dP);
		dL += calculateDangerFromEnemyBots(time, dP);
		dL += calculateDangerFromEnemyWaves(time, dP);
		return dL;
	}

	public double calculateDangerFromEnemyWaves(long time, dangerPoint dP) {
		double dL = 0;
		for ( waveWithBullets eW : myBot.getEnemyWaves() ) {
			dL += eW.getDanger( time, dP.getPosition() );
		}
		return dL;
	}

	public double calculateDangerFromEnemyBots(long time, dangerPoint dP) {
		double dL = 0;
		for ( fighterBot eB : myBot.getEnemyBots() ) {
			dL += eB.getDanger( time, dP.getPosition() );
		}
		return dL;
	}

	public double calculateDangerFromWall(long time, dangerPoint dP) {
		double dLWall = 1e8; // very high
		double dL = 0;
		double dist = physics.shortestDist2wall( dP.getPosition() );
		if ( dist <= 1.0*physics.robotHalfSize ) {
			dL = dLWall;
		}
		return dL;
	}

	public void sortDangerPoints() {
		Collections.sort(dangerPoints);
	}

	public dangerPoint getSafestPoint() {
		sortDangerPoints();
		return dangerPoints.getFirst();
	}

	public void clearDangerPoints() {
		dangerPoints.clear();
	}


	public void onPaint(Graphics2D g) {
		for( dangerPoint dP: dangerPoints ) {
			dP.onPaint(g);
		}
	}
}
