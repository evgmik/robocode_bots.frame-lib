// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import eem.frame.external.trees.secondGenKD.KdTree;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class kdtreeGuessFactorGun extends guessFactorGun {
	int neigborsNum = 400;
	String kdTreeGunBaseName = "kdtGF";

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
		return gTP.getPosition();
	}

	@Override
	protected double[] getRelevantGF( fighterBot fBot, InfoBot tBot ) {
		KdTree<gfHit> tree = getKdTree( fBot, tBot );
		double[] gfBins = new double[ fBot.getGunManager().getGuessFactosrBinNum() ];
		double[] coord = getTreePointCoord();
		if ( coord == null ) {
			logger.error("error: this should not happen -  coord for KdTree is null");
			return gfBins;
		}

		boolean isSequentialSorting = false;
		List<KdTree.Entry<gfHit>> cluster = tree.nearestNeighbor( coord, neigborsNum, isSequentialSorting );

		//logger.dbg(getName() + " kdTree has " + tree.size() + " nodes");
		for ( KdTree.Entry<gfHit> neigbor : cluster ) {
			gfBins[neigbor.value.gfBin] += neigbor.value.weight; // fixme do gf  weights  and distances
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

		return gfBins;
	}

}

