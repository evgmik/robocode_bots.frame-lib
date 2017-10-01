// -*- java -*-
package eem.frame.misc;

public class ArrayWithMath {
	public double[] bins;
	public int length = 0;

	public ArrayWithMath( double[] bins ) {
		length = bins.length;
		this.bins = new double[length];
		for (int i=0; i < length; i++ ) {
			this.bins[i] = bins[i];
		}
	}

	public ArrayWithMath plus(double x) {
		for (int i=0; i < length; i++ ) {
			bins[i] += x;
		}
		return this;
	}

	public ArrayWithMath multiplyBy(double x) {
		for (int i=0; i < length; i++ ) {
			bins[i] *= x;
		}
		return this;
	}

	public ArrayWithMath plus(ArrayWithMath array2) {
		int N=length;
		if (this.length != array2.length ) {
			logger.error( "ERROR: arrays have different sizes. Adding smallest set");
			N=Math.min(N, array2.length);
		}
		for (int i=0; i < N; i++ ) {
			bins[i] += array2.bins[i];
		}
		return this;
	}
}
