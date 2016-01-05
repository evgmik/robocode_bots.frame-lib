// -*- java -*-

package eem.frame.gun;

public class gunHitMissLog {
	public boolean hitStat = false; 
	public double weight = 1;

	public gunHitMissLog() {
	}

	public gunHitMissLog( boolean hS, double w ) {
		hitStat = hS;
		weight = w;
	}
}

