// -*- java -*-
package eem.frame.misc;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.LinkedList;

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
	public static LinkedList<String> methodsChain   = new LinkedList<String>();

	private long startTime;
	private long totalExecTime = 0;
	private long minExecTime = Long.MAX_VALUE; ;
	private long maxExecTime = Long.MIN_VALUE; ;
	private int  numExec = 0;
	private boolean isActive = false;
	private boolean showYourSelf = true;
	private boolean showChildren = true;
	public static boolean SHOW_CHILDREN = true;

	public static String longName() {
		String sep = ".";
		String str = "";
		for (String m: methodsChain) {
			str += sep;
			str += m;
		}
		return str;
	}

	public static void start( String methodName ) {
		String parentName = longName();
		profiler parent = profilers.get(parentName);
		methodsChain.add(methodName);
		String name = longName();
		profiler p = profilers.get(name);
		if ( p == null ) {
			p = new profiler();
			profilers.put(name, p );
			if ( parent != null ) {
				p.showYourSelf = parent.showChildren;
				p.showChildren = parent.showChildren;
			}
		}
		p.startTime = System.nanoTime();
		if (!p.isActive) {
			p.isActive = true;
			p.numExec ++;
		} else {
			logger.error("ERROR: profiler restarted without stopping for method " + name);
		}
	}

	public static void start( String methodName, boolean showChildren ) {
		profiler.start( methodName );
		String name = longName();
		profiler p = profilers.get(name);
		if ( p == null ) {
			logger.error("ERROR: something wrong was not able to start profiler with name " + name);
		}
		p.showChildren = showChildren;
	}

	public static void stop( String methodName ) {
		String name = longName();
		profiler p = profilers.get(name);
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
		p.isActive = false;
		methodsChain.removeLast();
	}

	public static String formatHeaders() {
		String sep = " | ";
		String margin = "  ";
		String str = margin;
		str += String.format("%10s", "times exec");
		str += sep;
		str += String.format("%8s", "min");
		str += sep;
		str += String.format("%8s", "average");
		str += sep;
		str += String.format("%8s", "max");
		str += sep;
		str += String.format("%8s", "total");
		str += sep;
		str += "method name";
		return str;
	}

	public static String format( String methodName ) {
		String sep = " | ";
		String margin = "  ";
		String str = "";
		profiler p = profilers.get(methodName);
		if ( p == null ) {
			// this method did not start its clock
			str += "\n";
			str += "Method " + methodName + " was never executed";
		} else {
			if ( p.numExec >= 1 && p.showYourSelf ) {
				str += "\n";
				str += margin;
			       	str += String.format("%10s", p.numExec);
				str += sep;
			       	str += String.format("%8s", profTimeString(p.minExecTime) );
				str += sep;
			       	str += String.format("%8s", profTimeString(p.totalExecTime/p.numExec) );
				str += sep;
			       	str += String.format("%8s", profTimeString(p.maxExecTime) );
				str += sep;
			       	str += String.format("%8s", profTimeString(p.totalExecTime) );
				str += sep;
				str +=  methodName;
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
		if (t > 1000*1000*1000)
			return sign + String.format("%.1f", t/(1000.*1000*1000) ) + "  S";
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
		str += "\n";
		str += formatHeaders();
		for ( String k : keys ) {
			str += format( k );
		}
		str += "\n";
		str += "---------------------------";
		return str;
	}
}

