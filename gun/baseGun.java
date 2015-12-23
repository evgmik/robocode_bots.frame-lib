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
	long infoDelayTimeThreshold = (long) (360/robocode.Rules.RADAR_TURN_RATE + 1);

	public baseGun() {
		gunName = "baseGun";
	}

	public String getName(){
		return gunName;
	}

	public LinkedList<firingSolution> getFiringSolutions( InfoBot fBot, InfoBot tBot, long time, double bulletEnergy ) {
		return new LinkedList<firingSolution>();
	}

	public void setDistanceAtLastAimFor( firingSolution fS, Point2D.Double fPos, Point2D.Double tPos ) {
		fS.setDistanceAtLastAim( fPos.distance( tPos ) );
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
		double p = 1;
		long maxOkLag = 0;
		if ( infoLagTime <= maxOkLag  ) {
			// <= 0 time point from the future
			p = 1.0; // 1 is the best solution
		} else {
			// we are using outdated info
			p = Math.exp( -(infoLagTime-maxOkLag)/(3*infoDelayTimeThreshold) );
		}
		return p;
	}

	public firingSolution correctForInWallFire( firingSolution fS ) {
		// when a firing bot close to the wall,
		// it is better to shoot along the wall all the way
		Point2D.Double fP =  fS.getFiringPositon();
		double fA = math.shortest_arc( fS.getFiringAngle() );

		double dl = 1; // small uncertainty
		double botW = 2*physics.robotHalfSize - dl;;
		double bfW = physics.BattleField.x;
		double bfH = physics.BattleField.y;
		
		double tmpfA;
		// Now,  we correct the angle if needed
		if ( fP.x <= botW ) {
			// firing point is near left wall
			tmpfA = math.putWithinGameArc( fA, 
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( botW, 0 ))),
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( botW,  bfH )))
				);
			if ( tmpfA != fA ) {
				fA = tmpfA;
			}
		}
		if ( fP.x >= (bfW-botW) ) {
			// firing point is near right wall
			tmpfA = math.putWithinGameArc( fA, 
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( bfW-botW, bfH ))),
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( bfW-botW, 0 )))
				);
			if ( tmpfA != fA ) {
				fA = tmpfA;
			}
		}
		if ( fP.y >= (bfH-botW) ) {
			// firing point is near top wall
			tmpfA = math.putWithinGameArc( fA, 
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( 0, bfH-botW ))),
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( bfW, bfH-botW )))
				);
			if ( tmpfA != fA ) {
				fA = tmpfA;
			}
		}
		if ( fP.y <= botW ) {
			// firing point is near bottom wall
			tmpfA = math.putWithinGameArc( fA, 
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( bfW, botW ))),
				math.shortest_arc( math.angle2pt( fP, new Point2D.Double( 0, botW )))
				);
			if ( tmpfA != fA ) {
				fA = tmpfA;
			}
		}
		fS.setFiringAngle( fA );
		return fS;
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

