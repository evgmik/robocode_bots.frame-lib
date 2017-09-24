// -*- java -*-

package eem.frame.dangermap;

import eem.frame.core.*;
import eem.frame.motion.*;
import eem.frame.dangermap.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import robocode.util.*;

import java.util.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.awt.Color;



public class dangerPath implements Comparable<dangerPath> {
	private LinkedList<dangerPathPoint> path = new LinkedList<dangerPathPoint>();
	private double dangerLevel = 0;

	public dangerPath(){};

	public dangerPath( LinkedList<botStatPoint> bStatsList){
		double dL = 0;
		path = new LinkedList<dangerPathPoint>();
		ListIterator<botStatPoint> iter = bStatsList.listIterator();
		botStatPoint  bSt;
		while (iter.hasNext()) {
			bSt = iter.next();
			path.add ( new dangerPathPoint( bSt, dL ) );
		}
	};

	public void add(dangerPathPoint dP) {
		path.add( dP );
		dangerLevel += dP.getDanger();
	}

	public int size() {
		return path.size();
	}

	public dangerPathPoint get(int i) {
		if ( i >= size() )
			return null;
		return path.get(i);
	}

	public double getDanger() {
		return dangerLevel;
	}

	public void setDanger( double dL) {
		dangerLevel = dL;
	}

	public dangerPathPoint removeFirst() {
		dangerPathPoint dP = path.removeFirst();
		dangerLevel -= dP.getDanger();
		return dP;
	}

	public dangerPathPoint removeLast() {
		dangerPathPoint dP = path.removeLast();
		dangerLevel -= dP.getDanger();
		return dP;
	}


	public dangerPathPoint getFirst() {
		dangerPathPoint dP = path.getFirst();
		return dP;
	}

	public dangerPathPoint getLast() {
		dangerPathPoint dP = path.getLast();
		return dP;
	}

	public void shortenToWaveHit() {
		LinkedList<dangerPathPoint> shortPath = new LinkedList<dangerPathPoint>();
		ListIterator<dangerPathPoint> iter = path.listIterator();
		dangerPathPoint  dP;
		int cnt = 0;
		while (iter.hasNext()) {
			cnt++;
			dP = iter.next();
			shortPath.add(dP);
			if (dP.onTheWave)
				break;
		}
		shortenTo( cnt );
	}

	public void shortenTo( int N ) { // make path N elements long
		LinkedList<dangerPathPoint> shortPath = new LinkedList<dangerPathPoint>();
		ListIterator<dangerPathPoint> iter = path.listIterator();
		dangerPathPoint  dP;
		int cnt = 0;
		while (iter.hasNext()) {
			cnt++;
			dP = iter.next();
			shortPath.add(dP);
			if ( cnt >= N)
				break;
		}
		path = shortPath;
	}

	public void truncateToSafestPoint() {
		ListIterator<dangerPathPoint> iter = path.listIterator();
		double smallestDanger = Double.POSITIVE_INFINITY;
		dangerPathPoint  dP = null;
		int cnt = 0;
		int safestPointIndex=0;
		while (iter.hasNext()) {
			dP = iter.next();
			if ( dP.getDanger() < smallestDanger ) {
				smallestDanger = dP.getDanger();
				safestPointIndex = cnt;
			}
			cnt++;
		}
		shortenTo( safestPointIndex + 1);
	}

	public int compare(dangerPath p1, dangerPath p2) {
		double dL1 = p1.getDanger();
		double dL2 = p2.getDanger();
		if ( dL1 == dL2 ) return 0;
		if ( dL1 >  dL2 ) return 1;
		return -1;
	}

	public int compareTo( dangerPath p2) {
		return compare( this, p2);
	}

	public double calculateDanger(fighterBot myBot, double DoNotExceedDanger) {
		// this method stops if intermideate agregated danger exeeds DoNotExceedDanger
		// this intended to not waste CPU if we already have a better path
		profiler.start( "calculateDanger" );
		dangerPathPoint  dP;
		double dL = 0;
		ListIterator<dangerPathPoint> iter = path.listIterator();
		while (iter.hasNext()) {
			dP = iter.next();
			dL += dP.calculateDanger( myBot );
			if ( dL > DoNotExceedDanger)
				break; // no point to calculate further
		}
		setDanger(dL);
		profiler.stop( "calculateDanger" );
		return dL;
	}

	public void print() {
		ListIterator<dangerPathPoint> iter = path.listIterator();
		dangerPathPoint oldP=null;
		dangerPathPoint  dP;
		while (iter.hasNext()) {
			dP = iter.next();
			logger.dbg( dP.toString() );
		}
		logger.dbg("Path danger = " + dangerLevel);
	}

	public void onPaint(Graphics2D g) {
		ListIterator<dangerPathPoint> iter = path.listIterator();
		dangerPathPoint oldP=null;
		dangerPathPoint  dP;
		while (iter.hasNext()) {
			dP = iter.next();

			// path between points
			g.setColor(Color.blue);
			if ( oldP != null ) {
				graphics.drawLine(g,  dP.getPosition(), oldP.getPosition());
			}
			oldP = dP;

			dP.onPaint(g);
		}
	}
}

