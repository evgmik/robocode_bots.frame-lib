// -*- java -*-

package eem.frame.core;

public interface proxyBotInterface {

	// this should give or null access to realbot commands

	// radar
	public void setTurnRadarRight(double a);
	public void setAdjustRadarForGunTurn(boolean c);
	public double getRadarHeading();

	// body
	public void setTurnRight(double a);
	public void setAhead(double d);


	// gun
	public void setTurnGunRight(double a);
	public void setFireBullet(double e);
	public double getGunHeading();
	public double getGunTurnRemaining();
	public double getGunHeat();



}


