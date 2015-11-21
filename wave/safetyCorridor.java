// -*- java -*-

package eem.frame.wave;

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
}
