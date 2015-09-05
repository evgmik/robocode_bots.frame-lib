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

	public void setColor( Color c ) {
		color = c;
	}

	public Color getColor() {
		return color;
	}
}

