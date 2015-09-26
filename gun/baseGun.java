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

	public LinkedList<firingSolution> setFiringBotName( String name, LinkedList<firingSolution> fSols ) {
		for( firingSolution fS : fSols ) {
			fS.setFiringBotName( name );
		}
		return fSols;
	}

	public LinkedList<firingSolution> setTargetBotName( String name, LinkedList<firingSolution> fSols ) {
		for( firingSolution fS : fSols ) {
			fS.setTargetBotName( name );
		}
		return fSols;
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

	public Point2D.Double shiftFromDirectLine( Point2D.Double fP, Point2D.Double originalTP ){
		Point2D.Double tP = new Point2D.Double( originalTP.getX(), originalTP.getY() );
		// some bots like DrussGT use fire shield against simple guns
		// this should help byllets to sneak through the shield
		double offAngle = 3*math.signNoZero( Math.random() -0.5 );
		double angle = math.angle2pt(fP, tP);
		double dist = fP.distance(tP);
		tP.x += dist*Math.sin( Math.toRadians( angle + offAngle ) );
		tP.y += dist*Math.cos( Math.toRadians( angle + offAngle ) );
		return tP;
	}

	public void setColor( Color c ) {
		color = c;
	}

	public Color getColor() {
		return color;
	}
}

