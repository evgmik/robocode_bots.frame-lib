// -*- java -*-

package eem.frame.gun;
import eem.frame.misc.*;

import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class firingSolution {
	public double firingAngle;
	public long firedTime;
	public double bulletEnergy;
	public Point2D.Double firingPosition;
	public Point2D.Double targetPosition;
	public String gunName = "";
	public Color color = new Color(0x00, 0x00, 0x00, 0xff); // default color
	// Guns algorithm should set qualityOfSolution.
	// The idea bihind that some guns need sertain number of info points in a raw
	// (pattern matcher would be an example)
	// others would produce outdated solutions if they fire based on outdated info
	// (for example if linear gun use old info 
	// the targer bot might change the direction by that time).
	// qualityOfSolution = 1 is the best
	// qualityOfSolution = 0 is the worst
	public double qualityOfSolution = 0; 

	public firingSolution() {
		firingAngle = Double.NaN;
		firingPosition = null;
		targetPosition = null;
	}

	public firingSolution( Point2D.Double fP, double angle, long time, double bulletEnergy ) {
		firingAngle = angle;
		firingPosition = (Point2D.Double) fP.clone();
		firedTime = time;
		targetPosition = null;
		this.bulletEnergy = bulletEnergy;
	}

	public firingSolution( Point2D.Double fP, Point2D.Double tP, long time, double bulletEnergy) {
		firingAngle = math.angle2pt( fP, tP );
		firingPosition = (Point2D.Double) fP.clone();
		firedTime = time;
		targetPosition = (Point2D.Double) tP.clone();
		this.bulletEnergy = bulletEnergy;
	}

	public firingSolution(baseGun gun, Point2D.Double fP, Point2D.Double tP, long time, double bulletEnergy) {
		this( fP, tP, time, bulletEnergy );
		setGunName( gun.getName() );
		setColor( gun.getColor() );
	}

	public void setColor( Color c ) {
		color = c;
	}

	public Color getColor() {
		return color;
	}

	public void setGunName( String name ) {
		gunName = name;
	}

	public String getGunName() {
		return gunName;
	}
	public double smallestDistanceToBulletPath( Point2D.Double p ) {
		double a = math.game_angles2cortesian( firingAngle );

		// unit velocity vector ( i.e path vector )
		double vx = Math.cos( Math.toRadians(a) );
		double vy = Math.sin( Math.toRadians(a) );

		// vector to the point of interest
		double dx = p.x - firingPosition.x;
		double dy = p.y - firingPosition.y;

		// closest distance is given by vector product v x d
		double dist;
		dist = Math.abs( vx*dy - vy*dx );

		return dist;
	}

	public double getDanger( long time, Point2D.Double dP ) {
		// let's find the danger of this point by finding the minimal
		// distance from bullet path to the point of interest
		double dL = 0;
		double dangerRadius = physics.robotHalfSize;
		double bulletDanger = 1;

		double dist;

		//dist = smallestDistanceToBulletPath( dP ); // this is good for precursors
		dist = getLocationAt( time ).distance(dP); // bullet vicinity danger

		if ( dist <= Math.sqrt(2) * physics.robotHalfSize )
			dL += bulletDanger;
		dL += bulletDanger * Math.exp( - dist/ dangerRadius );

		return dL;
	}

	public double getDistanceTraveledAtTime(long time) {
		double timeInFlight = time - firedTime;
		double distTraveled = timeInFlight * physics.bulletSpeed( bulletEnergy );
		return distTraveled;
	}

	public void setQualityOfSolution(double q) {
		qualityOfSolution = q;
	}

	public double getQualityOfSolution() {
		return qualityOfSolution;
	}

	public Point2D.Double getLocationAt( long time ) {
			double dist = getDistanceTraveledAtTime( time );
			// note that dx and dy change meaning due to robocode coordinates
			double a = math.game_angles2cortesian( firingAngle );
			double dx = dist*Math.cos( Math.toRadians(a) );
			double dy = dist*Math.sin( Math.toRadians(a) );
			Point2D.Double endP = new Point2D.Double( firingPosition.x + dx, firingPosition.y + dy );
			return endP;
	}

	public boolean didItHitBotAtPos( Point2D.Double p, long time ) {
		Point2D.Double bulletPos = getLocationAt( time );
		if ( p.distance( bulletPos ) <= physics.robotHalfDiagonal ) {
			return true;
		} else {
			return false;
		}
	}

	public String toString() {
		String str = "";
		str += "Firing solultion at time: " + firedTime + "\n";
		str += "qualityOfSolution " + qualityOfSolution + "\n";
		str += "firingPosition " + firingPosition + ":\n";
		str += "firingAngle " + firingAngle + ":\n";
		if ( targetPosition != null ) {
			str += "targerPosition " + targetPosition + ":\n";
		}
		return str;
	}

	public void onPaint(Graphics2D g, long time) {
		if ( firingPosition == null ) {
			logger.error( "This should not happen: the firing solution does not have firingPosition" );
		} else {
			g.setColor( color );
			graphics.drawRect( g, firingPosition, 20, 20 );
		}

		if ( targetPosition != null ) {
			g.setColor( color );
			graphics.drawRect( g, targetPosition, 20, 20 );
		}

		if ( firingAngle == Double.NaN ) {
			logger.error( "This should not happen: the firing solution does not have firingAngle" );
		} else {
			g.setColor( color );
			Point2D.Double endP = getLocationAt( time );
			graphics.drawLine( g, firingPosition,  endP );
		}
	}

	public void onPaint(Graphics2D g ) {
			logger.dbg( "This should not happen: the firing solution does not have firingPosition" );
		if ( firingPosition == null ) {
			logger.error( "This should not happen: the firing solution does not have firingPosition" );
		} else {
			g.setColor( color );
			graphics.drawRect( g, firingPosition, 20, 20 );
		}

		if ( targetPosition != null ) {
			g.setColor( color );
			graphics.drawRect( g, targetPosition, 20, 20 );
		}

		if ( firingAngle == Double.NaN ) {
			logger.error( "This should not happen: the firing solution does not have firingAngle" );
		} else {
			
			g.setColor( color );
			if ( targetPosition != null ) {
				graphics.drawLine( g, firingPosition,  targetPosition );
			} else {
				double L = 30;
				double a = math.game_angles2cortesian( firingAngle );
				double dx = Math.cos( Math.toRadians(a) );
				double dy = Math.sin( Math.toDegrees(a) );
				Point2D.Double endP = new Point2D.Double( firingPosition.x + dx, firingPosition.y + dy );
				graphics.drawLine( g, firingPosition,  endP );
			}
		}
	}
}

