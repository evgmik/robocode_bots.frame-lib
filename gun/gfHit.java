// -*- java -*-

package eem.frame.gun;

public class gfHit {
	public int gfBin; // which bin
	public double weight; // weight of this gf, i.e. how much we trust it
	public double gfCoverage; // the bot shadow, i.e how many nearby bins are affected
	public long   firedTime;  // time when the gun with this hit was fired

	public gfHit( int b, double w ) {
		gfBin = b;
		weight = w;
		gfCoverage = 1;
	}

	public gfHit( int b, double w, double gfC, long fT ) {
		gfBin = b;
		weight = w;
		gfCoverage = gfC;
		firedTime = fT;
	}
}

