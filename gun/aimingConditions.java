// -*- java -*-

package eem.frame.gun;
import eem.frame.bot.*;
import eem.frame.misc.*;

public class aimingConditions {
	public String firingBotName="", targetBotName="";
	public long timeWhenCalculated=0, fireTime=0;
	public double bulletEnergy=0;

	public aimingConditions(fighterBot fBot, InfoBot tBot, long fireTime, double bulletEnergy ) {
		firingBotName = fBot.getName();
		targetBotName = tBot.getName();
		timeWhenCalculated = fBot.getTime();
		this.fireTime = fireTime;
		this.bulletEnergy = bulletEnergy;
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public String toString() {
		String s = "";
		s = "At time " + timeWhenCalculated 
			+ " " + firingBotName 
			+ " aims at " + targetBotName
			+ " to fire at " + fireTime 
			+ " bullet with energy " + bulletEnergy;
		return s;
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof aimingConditions)) return false;
		aimingConditions other = (aimingConditions) o;
		if (! this.firingBotName.equals( other.firingBotName ) ) return false;
		if (! this.targetBotName.equals( other.targetBotName ) ) return false;
		if ( this.timeWhenCalculated != other.timeWhenCalculated ) return false;
		if ( this.fireTime != other.fireTime ) return false;
		if ( this.bulletEnergy != other.bulletEnergy ) return false;
		return true;
	}
}	
