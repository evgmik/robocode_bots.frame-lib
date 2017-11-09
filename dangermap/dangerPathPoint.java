// -*- java -*-

package eem.frame.dangermap;

import eem.frame.bot.*;
import eem.frame.misc.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class dangerPathPoint implements Comparable<dangerPathPoint> {
	public botStatPoint botStat;
	public double dangerLevel;
	public boolean onTheWave = false;

	public dangerPathPoint() {
		botStat  = new botStatPoint();
		dangerLevel = 0;
	}

	public dangerPathPoint( botStatPoint bSt, double dL ) {
		botStat = bSt;
		dangerLevel = dL;
	}

	public double getDanger() {
		return dangerLevel;
	}

	public botStatPoint getBotStatPoint() {
		return botStat;
	}

	public double calculateDanger( fighterBot myBot ) {
		//profiler.start( "dangerPathPoint_calculateDanger" );
		double dL = 0;
		double[] dangers = new double[6];
		long time = botStat.getTime();
		//profiler.start( "SpacialPositionDanger" );
		dangers[0] = dangerCalc.calculateDangerFromCorners(time, botStat.getPosition(), myBot);
		dangers[1] = dangerCalc.calculateDangerFromWall(time, botStat.getPosition(), myBot);
		if ( myBot.getEnemyBots().size() == 4 ) {
			// It is very bad to be in the center of crossfire of 4 enemies,
			// where the master bot is the closest to all of them.
			dangers[2] = dangerCalc.calculateDangerFromCenter(time, botStat.getPosition(), myBot);
		} else {
			dangers[2] = 0;
		}
		//profiler.stop( "SpacialPositionDanger" );
		//profiler.start( "OtherBotsDanger" );
		dangers[3] = dangerCalc.calculateDangerFromEnemyBots(time, botStat.getPosition(), myBot);
		//profiler.stop( "OtherBotsDanger" );
		//profiler.start( "WaveDanger" );
		dangers[4] = dangerCalc.calculateDangerFromEnemyWaves(time, this, myBot);
		if (dangers[4] != 0) {
			onTheWave = true;
		}
		// call it after WaveDanger check, it uses onTheWave status
		dangers[5] = dangerCalc.calculateDangerFromSlowMotion(time, this, myBot);
		//profiler.stop( "WaveDanger" );
		//logger.dbg( "dangers " + logger.arrayToTextPlot( dangers ) + " at time " + time + " at pos " + botStat.getPosition() );
		//profiler.start( "SummingUp" );
		ArrayStats stats = new ArrayStats(dangers);
		dL = stats.sum;
		setDanger(dL);
		//profiler.stop( "SummingUp" );
		//profiler.stop( "dangerPathPoint_calculateDanger" );
		return dL;
	}
	
	public void setDanger(double dL) {
		dangerLevel = dL;
	}

	public Point2D.Double getPosition() {
		return botStat.getPosition();
	}

	public void setPosition( Point2D.Double p ) {
		botStat.setPosition( (Point2D.Double) p.clone() );
	}

	public int compare(dangerPathPoint p1, dangerPathPoint p2) {
		double dL1 = p1.dangerLevel;
		double dL2 = p2.dangerLevel;
		if ( dL1 == dL2 ) return 0;
		if ( dL1 >  dL2 ) return 1;
		return -1;
	}

	public int compareTo( dangerPathPoint p2) {
		return compare( this, p2);
	}

	public String toString() {
		String str = "";
		str += "Point " + botStat.format() + "\n" + " has danger level = " + dangerLevel;
		return str;
	}

	public void onPaint(Graphics2D g) {
		Point2D.Double p;
		p = this.botStat.getPosition();
		double dL = this.dangerLevel;
		g.setColor( graphics.dangerLevel2mapColor( dL ) );
		// circle surroundig point, representing its danger
		double pR = 5;
		graphics.drawCircle(g, p, pR);
		// put dot in the middle
		g.setColor( new Color(0x00, 0x00, 0xaa, 0xff) );
		double dR = 2;
		graphics.drawCircle(g, p, dR);
		if ( onTheWave ) {
			g.setColor( graphics.dangerLevel2mapColor( dL ) );
			graphics.drawSquare(g, p, 40);
			g.setColor( new Color(0xff, 0xff, 0xff, 0xff) );
			graphics.drawSquare(g, p, 42);
		}
	}

}
