// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class baseGun {
	public String gunName;
	public Color color = new Color(0x00, 0x00, 0x00, 0xff); // default color

	public baseGun() {
		gunName = "baseGun";
	}

	public String getName(){
		return gunName;
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		return new LinkedList<firingSolution>();
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		return new LinkedList<firingSolution>();
	}

	public double getLagTimePenalty( long infoLagTime ) {
		if ( infoLagTime <= 0  ) {
			// time point from the future
			return 1.0; // 1 is the best solution
		} else {
			// we are using outdated info
			return Math.exp(infoLagTime/5);
		}
	}

	public void setColor( Color c ) {
		color = c;
	}

	public Color getColor() {
		return color;
	}
}

