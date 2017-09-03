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


public class realHitsGun extends kdtreeGuessFactorGun {
	
	public realHitsGun( int neigborsNum ) {
		this( neigborsNum, 1, false );
	}

	public realHitsGun( int neigborsNum, int binsSumThreshold ) {
		this( neigborsNum, binsSumThreshold, false );
	}

	public realHitsGun( int neigborsNum, int binsSumThreshold, boolean antiGFavoider ) {
		color = new Color(0xff, 0x00, 0x00, 0x80);
		//kdTreeGunBaseName = "kdRealHitsGF";
		this.kdTreeGunBaseName = "realHitsGun";
		this.neigborsNum = neigborsNum;
		this.binsSumThreshold = binsSumThreshold;
		this.antiGFavoider = antiGFavoider;
		this.gunName ="";
		if ( antiGFavoider ) 
			this.gunName += "Anti-";

		//this.gunName +=  kdTreeGunBaseName + neigborsNum;
		this.gunName =  this.kdTreeGunBaseName;
	}

	@Override
	protected KdTree<gfHit> getKdTree( fighterBot fBot, InfoBot tBot ) {
		KdTree<gfHit> tree = fBot.getGunManager().getRealHitsGFKDTreeMap( tBot.getName() );
		return tree;
	}
}
