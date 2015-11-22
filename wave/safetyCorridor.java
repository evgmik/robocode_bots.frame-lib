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

	public String toString() {
		String str = "";
		str += "minA " + minAngle + " maxA " + maxAngle ;
		return str;
	}
}
