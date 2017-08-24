// -*- java -*-

package eem.frame.gun;
import eem.frame.misc.*;
import eem.frame.bot.*;
import eem.frame.wave.*;
import eem.frame.gameInfo.*;

import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.Graphics2D;
import java.awt.Color;

public class firingSolution {
	public double firingAngle;
	public long firedTime;
	public double bulletEnergy;
	public boolean realBulletFlag = false;
	public String firingBotName = "";
	public String targetBotName = "";
	public Point2D.Double firingPosition;
	public Point2D.Double targetPosition;
	public String gunName = "";
	public double distanceAtLastAim = 0;
	public boolean activeFlag = true; // if this firing solution hit target set to false
	public boolean myWavePassedOverTargetFlag = false;
	public Color color = new Color(0x00, 0x00, 0x00, 0xff); // default color
	// Guns algorithm should set qualityOfSolution.
	// The idea behind that some guns need certain number of info points in a raw
	// (pattern matcher would be an example)
	// others would produce outdated solutions if they fire based on outdated info
	// (for example if linear gun use old info 
	// the target bot might change the direction by that time).
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

	public firingSolution(baseGun gun, Point2D.Double fP, double angle, long time, double bulletEnergy) {
		this( fP, angle, time, bulletEnergy );
		setGunName( gun.getName() );
		setColor( gun.getColor() );
	}

	public void offsetFiringAngle( double offset ) {
		// apply small shift to known firing angle
		// intended to use for penetration through an enemy bullet shield
		firingAngle += offset;
	}

	public void setFiringBotName( String name ) {
		firingBotName = name;
	}

	public String getFiringBotName() {
		return firingBotName;
	}

	public boolean isRealBullet() {
		return realBulletFlag;
	}

	public void setIsRealBulletFlag( boolean state) {
		realBulletFlag = state;
	}

	public double getFiringAngle() {
		return firingAngle;
	}

	public void setFiringAngle(double a) {
		firingAngle = a;
	}

	public Point2D.Double getFiringPositon() {
		return firingPosition;
	}

	public long getFiredTime() {
		return firedTime;
	}

	public double getBulletSpeed() {
		return physics.bulletSpeed( bulletEnergy );
	}

	public double getBulletEnergy() {
		return bulletEnergy;
	}

	public void setDistanceAtLastAim( double d ) {
		distanceAtLastAim = d;
	}

	public double getDistanceAtLastAim() {
		return distanceAtLastAim;
	}

	public void setTargetBotName( String name ) {
		targetBotName = name;
	}

	public String getTargetBotName() {
		return targetBotName;
	}

	public boolean isActive() {
		return activeFlag;
	}

	public void setActiveFlag( boolean status ) {
		activeFlag = status;
	}

	public boolean isMyWavePassedOverTargetFlag() {
		return myWavePassedOverTargetFlag;
	}

	public void setMyWavePassedOverTargetFlag( boolean status ) {
		myWavePassedOverTargetFlag = status;
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

	public double getDanger( long time, safetyCorridor botShadow ) {
		double dL =0;
		double bulletDanger = 1 * qualityOfSolution;
		if ( isItInCoridor( botShadow ) ) {
			dL = bulletDanger;
		}
		// additional danger which drops with angular distance,
		// this code give small increase in hit avoidance at least agains
		// aiming head on bots like rz.HawkOnFire
		double fA = math.angleNorm360( firingAngle );
		double bsA = ( botShadow.getMaxAngle() + botShadow.getMinAngle() )/2;
		double dA = Math.abs(fA - bsA);
		dL += bulletDanger * Math.exp( - 2*dA/botShadow.getCorridorSize() );

		return dL;
	}

	public double getDanger( long time, Point2D.Double dP ) {
		// let's find the danger of this point by finding the minimal
		// distance from bullet path to the point of interest
		double dL = 0;
		double dangerRadius = physics.robotHalfSize;
		double bulletDanger = 1 * qualityOfSolution;

		double dist;

		//dist = smallestDistanceToBulletPath( dP ); // this is good for precursors
		dist = getLocationAt( time ).distance(dP); // bullet vicinity danger

		if ( dist <= Math.sqrt(2) * physics.robotHalfSize )
			dL += bulletDanger;
		dL += bulletDanger * Math.exp( - dist/ dangerRadius );

		return dL;
	}

	public double getDistanceTraveledAtTime(double time) {
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

	public Point2D.Double getLocationAt( double time ) {
			double dist = getDistanceTraveledAtTime( time );
			// note that dx and dy change meaning due to robocode coordinates
			double a = math.game_angles2cortesian( firingAngle );
			double dx = dist*Math.cos( Math.toRadians(a) );
			double dy = dist*Math.sin( Math.toRadians(a) );
			Point2D.Double endP = new Point2D.Double( firingPosition.x + dx, firingPosition.y + dy );
			return endP;
	}

	public boolean didItHitBotAtPos( Point2D.Double p, long time ) {
		// attempting to do precise bot intersection
		// see http://robowiki.net/wiki/Waves/Precise_Intersection
		// and http://robowiki.net/wiki/Talk:Waves/Precise_Intersection#Smashing_it_down_to_bins
		// It boils down to: Each tick, the bullet moves forward, forming a line segment,
		// and it is checked if this line segment intersects the bot bounding box,
		// then the enemy bot moves.
		if ( p == null ) {
			logger.error( "ERROR: empty bot position should not happen!" );
			return false;
		}
		Point2D.Double bulletPos = getLocationAt( time );
		Point2D.Double bulletPosPrev = getLocationAt( time-1 );
		Line2D.Double  bulletPath = new Line2D.Double( bulletPosPrev, bulletPos );
		boolean status = bulletPath.intersects( p.x - physics.robotHalfSize, p.y - physics.robotHalfSize, 2*physics.robotHalfSize, 2*physics.robotHalfSize);
		return status;
	}

	public boolean isItInCoridor( safetyCorridor sC ) {
		double fA = math.angleNorm360( firingAngle );
		double dA = math.shortest_arc( sC.getMaxAngle() - sC.getMinAngle() );
		if ( dA < 0 ) {
			logger.error( "error: Safety corridor angle is less than 0, this should not happen!" );
		}
		if ( ( sC.getMinAngle() <= fA ) && ( fA <= (sC.getMinAngle()+dA) ) ) {
			return true;
		}
		return false;	
	}

	public String toString() {
		String str = "";
		str += "Firing solultion at time: " + firedTime + "\n";
		str += "target " + targetBotName + "\n";
		str += "gun " + gunName + "\n";
		str += "qualityOfSolution " + qualityOfSolution + "\n";
		str += "firingPosition " + firingPosition + ":\n";
		str += "firingAngle " + firingAngle + ":\n";
		if ( targetPosition != null ) {
			str += "targerPosition " + targetPosition + ":\n";
		}
		return str;
	}

	public void drawFiringPositon( Graphics2D g, long time) {
		if ( firingPosition == null ) {
			logger.error( "This should not happen: the firing solution does not have firingPosition" );
		} else {
			g.setColor( color );
			graphics.drawRect( g, firingPosition, 20, 20 );
		}
	}

	public void drawTargetPositon( Graphics2D g, long time) {
		if ( targetPosition != null ) {
			g.setColor( color );
			graphics.drawRect( g, targetPosition, 20, 20 );
		}
	}

	public void drawBulletPath( Graphics2D g, long time) {
		if ( Double.isNaN( firingAngle ) ) {
			logger.error( "This should not happen: the firing solution does not have firingAngle" );
		} else {
			g.setColor( color );
			Point2D.Double endP = getLocationAt( time );
			graphics.drawLine( g, firingPosition,  endP );
		}
	}

	public void drawBulletLocation( Graphics2D g, long time) {
		if ( Double.isNaN( firingAngle ) ) {
			logger.error( "This should not happen: the firing solution does not have firingAngle" );
		} else {
			g.setColor( color );
			Point2D.Double endP = getLocationAt( time );
			double R = 4;
			graphics.drawCircle( g, endP,  R );
		}
	}

	public void drawBulletDanger( Graphics2D g, long time ) {
		if ( Double.isNaN( firingAngle ) ) {
			logger.error( "This should not happen: the firing solution does not have firingAngle" );
		} else {
			g.setColor( color );
			double a = math.game_angles2cortesian( firingAngle );
			double dx;
			double dy;
			double dist;
			double R = 5;

			dist = R;
			dx = dist*Math.cos( Math.toRadians(a) );
			dy = dist*Math.sin( Math.toRadians(a) );

			Point2D.Double strtP = getLocationAt( time );
			strtP.x = strtP.x + dx;
			strtP.y = strtP.y + dy;

			dist = 20*qualityOfSolution;
			dx = dist*Math.cos( Math.toRadians(a) );
			dy = dist*Math.sin( Math.toRadians(a) );

			Point2D.Double endP = new Point2D.Double( strtP.x + dx, strtP.y + dy );
			graphics.drawLine( g, strtP,  endP );
		}
	}

	public void onPaint(Graphics2D g, long time) {
		//drawFiringPositon( g, time );
		//drawTargetPositon( g, time );
		//drawBulletPath( g, time );
		drawBulletLocation( g, time );
		drawBulletDanger( g, time );
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

		if ( Double.isNaN( firingAngle ) ) {
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

