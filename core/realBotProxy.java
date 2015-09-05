// -*- java -*-

package eem.frame.core;

import eem.frame.bot.*;
import eem.frame.misc.*;

public class realBotProxy extends botProxy {
	// this is proxy which gives access to main robocode AdvancedBot interfaces

	public realBotProxy() {
		super();
		proxyName = "realBotProxy";
	}

	public realBotProxy( CoreBot bot) {
		this();
		masterBot = bot;
	}


	// radar
	public void setTurnRadarRight(double a) {
		masterBot.setTurnRadarRight( a);
	}

	public void setAdjustRadarForGunTurn(boolean c) {
		masterBot.setAdjustRadarForGunTurn( c );
	}

	public double getRadarHeading() {
		return masterBot.getRadarHeading();
	}

	// body
	public void setTurnRight(double a){
		masterBot.setTurnRight(a);
	}

	public void setAhead(double d) {
		masterBot.setAhead(d);
	}

	// gun
	public void setTurnGunRight(double a){
		masterBot.setTurnGunRight( a );
	}

	public void setFireBullet(double e){
		masterBot.setFireBullet( e );
	}

	public double getGunHeading() {
		return masterBot.getGunHeading();
	}

	public double getGunTurnRemaining() {
		return masterBot.getGunTurnRemaining();
	}

	public double getGunHeat() {
		return masterBot.getGunHeat();
	}
}
