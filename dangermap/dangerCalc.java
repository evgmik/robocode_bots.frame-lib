// -*- java -*-

package eem.frame.dangermap;

import eem.frame.dangermap.*;
import eem.frame.misc.*;
import eem.frame.wave.*;
import eem.frame.bot.*;

import java.util.LinkedList;
import java.util.Collections;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class dangerCalc {
	// calculates dangers in a point from myBot (any fighterBot) point of view

	public static double calculateDangerFromEnemyWaves(long time, Point2D.Double dP, fighterBot myBot) {
		double dL = 0;
		for ( waveWithBullets eW : myBot.getEnemyWaves() ) {
			dL += eW.getDanger( time, dP );
			if ( ( myBot.getGameInfo().fightType().equals("1on1") || myBot.getGameInfo().fightType().equals("melee1on1") ) ) {
				break; // dbg do danger only from earliest wave
			}
		}
		return dL;
	}

	public static double calculateDangerFromEnemyBots(long time,  Point2D.Double dP, fighterBot myBot) {
		double dL = 0;
		for ( fighterBot eB : myBot.getEnemyBots() ) {
			dL += eB.getDanger( time, dP );
		}
		return dL;
	}

	public static double calculateDangerFromWall(long time, Point2D.Double dP, fighterBot myBot) {
		double dLWall = 1;
		double wallDangerRadius = 5;
		double dL = 0;
		double dist = physics.shortestDist2wall( dP );
		if ( dist <= physics.robotHalfSize ) {
			dL += dLWall;
		}
		//dL += dLWall*Math.exp( -(dist-physics.robotHalfSize)/wallDangerRadius );
		return dL;
	}

	public static double calculateDangerFromCenter(long time, Point2D.Double dP, fighterBot myBot) {
		double dLCenter = 1;
		double centerDangerRadius = 500;
		double dL = 0;
		double dist = dP.distance( physics.BattleField.x/2, physics.BattleField.y/2);
		dL += dLCenter*Math.exp( -dist/centerDangerRadius );
		return dL;
	}

	public static double calculateDangerFromCorners(long time, Point2D.Double dP, fighterBot myBot) {
		Point2D.Double corner;
		double cornerDanger = 1;
		double cornerDangerRadius = 30;
		double dL = 0;
		double dist = 0;

		if ( !( myBot.getGameInfo().fightType().equals("1on1") || myBot.getGameInfo().fightType().equals("melee1on1") ) ) {
			// in many bot situation being in a corner is fine
			return 0;
		}

		// bottom left
		dist = dP.distance( new Point2D.Double( physics.botReacheableBattleField.getMinX(), physics.botReacheableBattleField.getMinY() ) );
		dL += cornerDanger*Math.exp(-dist/cornerDangerRadius);

		// top left
		dist = dP.distance( new Point2D.Double( physics.botReacheableBattleField.getMinX(), physics.botReacheableBattleField.getMaxY() ) );
		dL += cornerDanger*Math.exp(-dist/cornerDangerRadius);
		
		// top right
		dist = dP.distance( new Point2D.Double( physics.botReacheableBattleField.getMaxX(), physics.botReacheableBattleField.getMaxY() ) );
		dL += cornerDanger*Math.exp(-dist/cornerDangerRadius);

		// bottom right
		dist = dP.distance( new Point2D.Double( physics.botReacheableBattleField.getMaxX(), physics.botReacheableBattleField.getMinY() ) );
		dL += cornerDanger*Math.exp(-dist/cornerDangerRadius);

		return dL;
	}
}

