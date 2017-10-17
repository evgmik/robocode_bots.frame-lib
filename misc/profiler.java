// -*- java -*-
package eem.frame.misc;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;

// The idea for this profiler was inspired by Xander.Cat and its framework class RunTimeLogger 
// http://robowiki.net/wiki/XanderCat and http://robowiki.net/wiki/XanderFramework
// by Scott Arnold,  though his is much more elaborated and powerful.
//
// This implementation essentially logs start and execution time.
// User must run           profiler.start("CodeUnderProfilingName");
// and then at the end     profiler.stop ("CodeUnderProfilingName");
// once you ready to see stats run either  profiler.format("CodeUnderProfilingName")
// or profiler.formatAll() if you want to see all stats (handy at the end of the turn.

public class profiler {
	public static HashMap<String, profiler> profilers   = new HashMap<String, profiler>();

	private long startTime;
	private long totalExecTime = 0;
	private long minExecTime = Long.MAX_VALUE; ;
	private long maxExecTime = Long.MIN_VALUE; ;
	private int  numExec = 0;
	private boolean isActive = false;

	public static void start( String methodName ) {
		profiler p = profilers.get(methodName);
		if ( p == null ) {
			p = new profiler();
			profilers.put(methodName, p );
		}
		p.startTime = System.nanoTime();
		p.isActive = true;
	}

	public static void stop( String methodName ) {
		profiler p = profilers.get(methodName);
		if ( p == null ) {
			// this method did not start its clock
			return;
		}
		if ( !p.isActive ) {
			// this method did not start its clock
			return;
		}
		long execTime = System.nanoTime() - p.startTime;
		p.totalExecTime += execTime;
		if ( p.maxExecTime < execTime ) {
			p.maxExecTime = execTime;
		}
		if ( p.minExecTime > execTime ) {
			p.minExecTime = execTime;
		}
		p.numExec ++;
		p.isActive = false;
	}

	public static String format( String methodName ) {
		String str = "";
		profiler p = profilers.get(methodName);
		if ( p == null ) {
			// this method did not start its clock
			str += "Method " + methodName + " was never executed";
		} else {
			if ( p.numExec >= 1 ) {
				str += "  " + methodName;
			       	str += " was executed " + p.numExec;
				str += " execution times:";
			       	str += " min " + profTimeString(p.minExecTime);
			       	str += ", average " + profTimeString(p.totalExecTime/p.numExec);
			       	str += ", max " + profTimeString(p.maxExecTime);
			       	str += ", total " + profTimeString(p.totalExecTime);
			} else {
				str += "Method " + methodName + " was never executed";
			}
		}
		return str;
	}

	public static String profTimeString(long t) {
		String sign="";
		if (t < 0) {
			t=-t;
			sign="-";
		}
		if (t > 1000*1000)
			return sign + String.format("%.1f", t/(1000.*1000) ) + " mS";
		if (t > 1000)
			return sign + String.format("%.1f", t/(1000.) ) + " uS";

		return sign + ((long) t) + " nS";
	}

	public static String formatAll( ) {
		String str = "Profiler stats";
		str += "---------------------------";
		Set<String> keysSet = profilers.keySet();
		String[] keys = keysSet.toArray(new String[0]);
		Arrays.sort(keys);
		for ( String k : keys ) {
			str += "\n";
			str += format( k );
		}
		str += "\n";
		str += "---------------------------";
		return str;
	}
}

