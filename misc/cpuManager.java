// -*- java -*-
package eem.frame.misc;


public class cpuManager {
	// reworked from CpuManager.java
	private final static boolean JAVA_9 = System.getProperty("java.version").startsWith("9");
	public static long cpuConstant = -1; // initially negative to trigger recalculation

	public static long getCpuConstant() {
		if ( cpuConstant < 0 ) {
			calcCpuConstant();
		}
		return cpuConstant;
	}

	public static void calcCpuConstant() {
		// estimate CPU constant
		long APPROXIMATE_CYCLES_ALLOWED = 6250;
		long testPeriodInMillis = 1; // keep it below 5mS which is typical cpuConstant
		long maxTestTimeInNanoS = testPeriodInMillis*1000000;
		double d = 0;
		long count = 0;
		//cpuConstant = -1; // this will reset calculation
		for (int i=0; i<3; i++) {
			long startTime = System.nanoTime();
			while( System.nanoTime() - startTime < maxTestTimeInNanoS ) {
				d += Math.hypot(Math.sqrt(Math.abs(log(Math.atan(Math.random())))), Math.cbrt(Math.abs(Math.random() * 10))) / exp(Math.random());
				count++;
			}
			long newCpuConstant = (long) 1.0*APPROXIMATE_CYCLES_ALLOWED*maxTestTimeInNanoS/Math.max(1, count);
			if ( cpuConstant < 0 || newCpuConstant < cpuConstant) {
				cpuConstant = newCpuConstant;
			}
		}
		//logger.dbg("Cpu constant = " + profiler.profTimeString(cpuConstant) );
	}

	// Work-around for bug #390
	// The Java 9 Math.log(x) methods is much faster than in Java 8
	private static double log(double x) {
		if (JAVA_9) {
			double d = 0;
			for (int i = 0; i < 6; i++) {
				d += Math.log(x);
			}
			return d;
		} else {
			return Math.log(x);
		}
	}

	// Work-around for bug #390
	// The Java 9 Math.exp(x) methods is much faster than in Java 8
	private static double exp(double x) {
		if (JAVA_9) {
			double d = 0;
			for (int i = 0; i < 62; i++) {
				d += Math.exp(x);
			}
			return d;
		} else {
			return Math.exp(x);
		}
}

}
