// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class decayingGuessFactorGun extends guessFactorGun {
	public decayingGuessFactorGun() {
		gunName = "decayingGuessFactorGun";
		color = new Color(0x00, 0xff, 0x00, 0xff);
		binsSumThreshold=.1;
	}

	@Override
	protected double[] getRelevantGF( fighterBot fBot, InfoBot tBot ) {
		return fBot.getGunManager().getDecayingGuessFactors( tBot.getName() );
	}

}

