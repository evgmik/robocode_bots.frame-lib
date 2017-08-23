// -*- java -*-
package eem.frame.misc;

public class ArrayStats {
	public double sum=0, sqSum=0, mean=0, std=0,
	       max=Double.NEGATIVE_INFINITY, min=Double.POSITIVE_INFINITY;
       	public int indMax=0, indMin=0, length=0, nonZeroBinsN=0;

	public ArrayStats( double[] bins ) {
		double b;
		length = bins.length;
		for (int i=0; i < length; i++ ) {
			b = bins[i];
			sum += b; // calculates total count
			sqSum += b*b;
			if ( b > max ) {
				max = b;
				indMax = i;
			}
			if ( b < min ) {
				min = b;
				indMin = i;
			}
			if ( b != 0 ) {
				nonZeroBinsN++;
			}
		}
		if ( length != 0 ) {
			mean = sum/length;
			std  = Math.sqrt( (sqSum - mean*mean)/length );
		} else {
			logger.error( "ERROR: Do not send empty arrays to get its stats" );
		}
	}
}
