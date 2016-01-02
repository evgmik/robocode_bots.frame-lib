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
	int neigborsNum = 1000;

	public kdtreeGuessFactorGun() {
		gunName = "kdtreeGuessFactorGun";
		color = new Color(0x66, 0xAA, 0x66, 0xff);
		binsSumThreshold=3;
	}

	@Override
	protected double[] calcTreePointCoord( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		double[] coord = misc.calcTreePointCoord( fBot, tBot, time, bulletEnergy, fBot.getGunManager().getKdTreeDims() );
		return coord;
	}

	@Override
	protected double[] getRelevantGF( fighterBot fBot, InfoBot tBot ) {
		KdTree<gfHit> tree = fBot.getGunManager().getTreeKDTreeMap( tBot.getName() );
		double[] gfBins = new double[ fBot.getGunManager().getGuessFactosrBinNum() ];
		double[] coord = getTreePointCoord();
		if ( coord == null ) {
			logger.error("error: this should not happen -  coord for KdTree is null");
			return gfBins;
		}

		boolean isSequentialSorting = false;
		List<KdTree.Entry<gfHit>> cluster = tree.nearestNeighbor( coord, neigborsNum, isSequentialSorting );

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

