// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class gunManager implements gunManagerInterface {
	public LinkedList<baseGun> gunList = new LinkedList<baseGun>();
	public fighterBot myBot;
	fighterBot targetBot = null;
	double firingSolutionQualityThreshold = .5;
	long fireAtTime = -9999; // gun manager will set this time
	firingSolution finalFiringSolution = null;

	public	gunManager() {
		gunList = new LinkedList<baseGun>();
		gunList.add( new linearGun() );
	}

	public	gunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public void manage() {
	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
		if ( targetBot != null ) {
			double R = 50;
			graphics.drawCircle( g, targetBot.getPosition(), R );
		}
	}
}
