// -*- java -*-

package eem.frame.wave;

import  eem.frame.misc.*;

public class safetyCorridor {
	// class which holds a range ends
	double minAngle = 0; // angles are in games angles (degrees)
	double maxAngle = 0;

	public safetyCorridor( double minA, double maxA ) {
		minAngle = minA;
		maxAngle = maxA;
	}

	public double getMinAngle() {
		return minAngle;
	}

	public double getMaxAngle() {
		return maxAngle;
	}

	public void normalize() {
		// arranging angles to give shortest clock wise arc in between
		// I cannot think of a situation where safety corridor is > 180 degrees
		double hitAngle2 = math.angleNorm360( maxAngle );
		double hitAngle1 = math.angleNorm360( minAngle );

		double dA = math.shortest_arc( hitAngle2 - hitAngle1 );
		if ( dA < 0 ) {
			minAngle = hitAngle2;
			maxAngle = hitAngle2 - dA;
		} else {
			minAngle = hitAngle1;
			maxAngle = hitAngle1 + dA;
		}
	}

	public double getCorridorSize() {
		return (maxAngle - minAngle);
	}

	public safetyCorridor getOverlap( safetyCorridor sC ) {
		// calculates overlap of two safety corridors
		profiler.start( "getOverlap" );

		safetyCorridor sCoverlap = null;

		// count angles with respect to the smallest minAngle.
		double refAngle = Math.min( this.getMinAngle(), sC.getMinAngle() );
		double sc1amin = this.getMinAngle() - refAngle;
		double sc1amax = this.getMaxAngle() - refAngle;
		double sc2amin = sC.getMinAngle() - refAngle;
		double sc2amax = sC.getMaxAngle() - refAngle;

		double minEnd = Math.max( sc1amin, sc2amin );

		if ( (minEnd > sc1amax) || (minEnd >sc2amax) ) {
			// no overlap 
			sCoverlap = null;
		} else {
			double maxEnd = Math.min( sc1amax, sc2amax );

			sCoverlap = new safetyCorridor( minEnd + refAngle, maxEnd + refAngle);
			sCoverlap.normalize();
		}

		profiler.stop( "getOverlap" );
		return sCoverlap;
	}

	public safetyCorridor getJoin( safetyCorridor sC ) {
		// joins two corridors
		// IMPORTANT: it assume that there is an overlap
		//profiler.start( "getJoin" );

		safetyCorridor sCjoin = null;

		// count angles with respect to the smallest minAngle.
		double refAngle = Math.min( this.getMinAngle(), sC.getMinAngle() );
		double sc1amin = this.getMinAngle() - refAngle;
		double sc1amax = this.getMaxAngle() - refAngle;
		double sc2amin = sC.getMinAngle() - refAngle;
		double sc2amax = sC.getMaxAngle() - refAngle;

		double minEnd = Math.min( sc1amin, sc2amin );
		double maxEnd = Math.max( sc1amax, sc2amax );

		sCjoin = new safetyCorridor( minEnd + refAngle, maxEnd + refAngle);
		sCjoin.normalize();

		//profiler.stop( "getJoin" );
		return sCjoin;
	}

	public String toString() {
		String str = "";
		str += "minA " + minAngle + " maxA " + maxAngle ;
		return str;
	}
}
