// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

import java.util.LinkedList;
import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


public class flipLastGuessFactorGun extends guessFactorGun {
	public flipLastGuessFactorGun() {
		gunName = "flipLastGuessFactorGun";
		color = new Color(0xff, 0xff, 0x00, 0xff);
		binsSumThreshold = .1;
	}

	@Override
	protected double[] getRelevantGF( fighterBot fBot, InfoBot tBot ) {
		double[] lastGF = fBot.getGunManager().getDecayingGuessFactors( tBot.getName() );
		double[] reversedGF = new double[ lastGF.length ];   
		int left = 0;
		int right = lastGF.length-1;
		while (left <= right) {
			// reversing array
			reversedGF[ left  ] = lastGF[ right ];
			reversedGF[ right ] = lastGF[ left  ];
			left++;
			right--;
		}

		return reversedGF;
	}

}

