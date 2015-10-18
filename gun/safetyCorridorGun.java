// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class safetyCorridorGun extends randomGun {
	public safetyCorridorGun() {
		gunName = "safetyCorridorGun";
		color = new Color(0x00, 0xff, 0xff, 0x80);
	}

	public LinkedList<firingSolution> setAllSolytionsQuality( LinkedList<firingSolution> fSols, double q ) {
		for ( firingSolution fS: fSols ) {
			fS.setQualityOfSolution( q );
		}
		return fSols;
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSols = super.getFiringSolutions( fBot, tBot, time, bulletEnergy);
		return fSols = setAllSolytionsQuality( fSols, -1 );
	}

	public LinkedList<firingSolution> getFiringSolutions( Point2D.Double fP, InfoBot tBot, long time, double bulletEnergy ) {
		LinkedList<firingSolution> fSols = super.getFiringSolutions( fP, tBot, time, bulletEnergy);
		return fSols = setAllSolytionsQuality( fSols, -1 );
	}
}

