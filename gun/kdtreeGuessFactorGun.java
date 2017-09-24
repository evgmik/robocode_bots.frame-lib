// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import eem.frame.external.trees.secondGenKD.KdTree;

import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class kdtreeGuessFactorGun extends guessFactorGun {
	int neigborsNum = 400;
	List<KdTree.Entry<gfHit>> cluster = null;
	String kdTreeGunBaseName = "kdtGF";
	boolean useCachedKdCluster = true;

	public kdtreeGuessFactorGun() {
		this( 400, 3 ); //default
	}

	public kdtreeGuessFactorGun( int neigborsNum ) {
		this( neigborsNum, 3, false ); //default
	}

	public kdtreeGuessFactorGun( int neigborsNum, boolean antiGFavoider ) {
		this( neigborsNum, 3, antiGFavoider );
	}

	public kdtreeGuessFactorGun( int neigborsNum, int binsSumThreshold, boolean antiGFavoider ) {
		this( neigborsNum, binsSumThreshold );
		this.antiGFavoider = antiGFavoider;
		this.gunName ="";
		if ( antiGFavoider ) 
			this.gunName += "Anti-";

		this.gunName +=  kdTreeGunBaseName + neigborsNum;
	}

	public kdtreeGuessFactorGun( int neigborsNum, int binsSumThreshold ) {
		color = new Color(0x66, 0xAA, 0x66, 0xff);
		this.neigborsNum = neigborsNum;
		this.binsSumThreshold = binsSumThreshold;
		this.gunName = kdTreeGunBaseName + neigborsNum;
	}

	protected KdTree<gfHit> getKdTree( fighterBot fBot, InfoBot tBot ) {
		return fBot.getGunManager().getTreeKDTreeMap( tBot.getName() );
	}

	@Override
	protected double[] calcTreePointCoord( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		gunTreePoint gTP = new gunTreePoint( fBot, tBot, time, bulletEnergy );
		treePointCoord = gTP.getPosition();
		if (!useCachedKdCluster) { cluster = null; return treePointCoord; } // exit here
		// otherwise prepare cached cluster
		HashMap<aimingConditions, List<KdTree.Entry<gfHit>> > kdClusterCache = fBot.getGunManager().getKdClusterCache();
		aimingConditions aC = new aimingConditions( fBot, tBot, time, bulletEnergy, kdTreeGunBaseName);
		cluster = kdClusterCache.get( aC );
		if ( cluster == null ) {
			//logger.dbg(getName() + " did not find cluster");
			kdClusterCache.clear();
			cluster = calculateNearestNeighborsCluster( fBot, tBot );
			kdClusterCache.put( aC, cluster);
		}
		return treePointCoord;
	}

	protected List<KdTree.Entry<gfHit>> calculateNearestNeighborsCluster( fighterBot fBot, InfoBot tBot ) {
		KdTree<gfHit> tree = getKdTree( fBot, tBot );
		double[] coord = getTreePointCoord();
		if ( coord == null ) {
			logger.error("error: this should not happen -  coord for KdTree is null");
			return null;
		}

		boolean isSequentialSorting = true; // if true, sort results from worst to best neighbors
		cluster = tree.nearestNeighbor( coord, neigborsNum, isSequentialSorting );
		return cluster;
	}

	@Override
	protected double[] getRelevantGF( fighterBot fBot, InfoBot tBot ) {
		//profiler.start(  getName() + " getRelevantGF.getCluster" );
		if ( !useCachedKdCluster ) {
			cluster = calculateNearestNeighborsCluster( fBot, tBot);
		}
		//profiler.stop(  getName() + " getRelevantGF.getCluster" );
		//profiler.start( getName() + " getRelevantGF.smoothGF" );
		double[] gfBins = new double[ fBot.getGunManager().getGuessFactosrBinNum() ];
		if ( cluster == null ) {
			return gfBins;
		}
		// FIXME: smoothing can be cached, high N neighbors gun already did it

		//logger.dbg(getName() + " kdTree has " + tree.size() + " nodes");
		int numGuessFactorBins = gfBins.length;
		double bestDistance = Double.POSITIVE_INFINITY;
		double dist = Double.POSITIVE_INFINITY;
		double distThreshold = 0.2;
		double scale =  0; // if we get zero neighbors
		int cnt=0;
		int Nresults = cluster.size();
		KdTree.Entry<gfHit> neigbor = null;
		if ( Nresults > 0 ) {
			// best neighbor
			neigbor = cluster.get( Nresults -1 );
			bestDistance = Math.max( neigbor.distance, distThreshold );
		}
		//since list is sorted from worst to best we have to do tricks
		//for ( KdTree.Entry<gfHit> neigbor : cluster ) {
		for ( int k = Nresults-1;  k>=0; k--) {
			neigbor = cluster.get( k );
			cnt++;
			if ( cnt > neigborsNum ) break; // counted enough neighbors
			scale =  binsSumThreshold; // if we here, at least 1 neighbor is found
			double binW0 = neigbor.value.weight; // fixme do gf  weights  and distances
			dist = Math.max( neigbor.distance, distThreshold );
			binW0 *= scale;
			binW0 *= bestDistance/dist;

			int iCenter = neigbor.value.gfBin;
			double di0 =     neigbor.value.gfCoverage;

			// smooth GF to neighbors
			int minI = (int)math.putWithinRange( iCenter - 2*di0, 0, (numGuessFactorBins-1) );
			int maxI = (int)math.putWithinRange( iCenter + 2*di0, 0, (numGuessFactorBins-1) );
			for ( int i = minI; i <= maxI; i++ ) {
				i = (int)math.putWithinRange( i, 0, (numGuessFactorBins-1) );

				double di = i-iCenter; // bin displacement from the center
				// every gf within (+/-)gfRange=di0 is a hit, so it should have
				// a weight close to 1. at 2*di0 we should have weight close to 1
				double binW = binW0 * Math.exp( - Math.pow( di/(1*di0) , 4 ) );
				gfBins[i]+= binW;
			}
		}

		if ( antiGFavoider ) {
			ArrayStats stats = new ArrayStats( gfBins );
			double max = stats.max;
			double[] gfBinsFlipped = new double[ numGuessFactorBins ];
			for (int i=0; i< numGuessFactorBins; i++) {
				// flipping GFs
				gfBinsFlipped[i] = max - gfBins[i];
			}

			double threshold = stats.mean*.5; // spill out weight into a GF bin
			// Essentially GF < threshold were never visited 
			// either we have too little stats  or it is unreachable GF.
			// Unreachable GF should be at edges we set them to zero so
			// they never checked
			// Threshold is quite high and tune heuristically to avoid shooting
			// extreme GFs.
			for (int i=0; i< numGuessFactorBins; i++) {
				// left edge search
				if ( gfBins[i] <= threshold ) {
					gfBinsFlipped[i] = 0;
				} else {
					break;
				}
			}
			for (int i=numGuessFactorBins-1; i>=0; i--) {
				// right edge search
				if ( gfBins[i] <= threshold ) {
					gfBinsFlipped[i] = 0;
				} else {
					break;
				}
			}
			// reassigning everything back to gfBins
			for (int i=0; i< numGuessFactorBins; i++) {
				gfBins[i] = gfBinsFlipped[i];
			}
		}

		if ( false ) { // enable for debugging
			int bestIndex = 0;
			double maxW = Double.NEGATIVE_INFINITY;
			int sum = 0;
			for ( int i=0; i < gfBins.length; i++ ) {
				double w = gfBins[i];
				if ( w > maxW ) {
					maxW = w;
					bestIndex = i;
				}
				sum += w;
			}
			logger.dbg( "gfIndex = " + bestIndex + " hit prob = " + maxW/sum );
		}

		//profiler.stop( getName() + " getRelevantGF.smoothGF" );
		return gfBins;
	}

}

