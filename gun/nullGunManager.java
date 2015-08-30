// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class nullGunManager extends gunManager {
	public nullGunManager() {
		gunList = new LinkedList<baseGun>();
	}

	public	nullGunManager(fighterBot bot) {
		this();
		myBot = bot;
	}
	public void manage() {
	}
}
