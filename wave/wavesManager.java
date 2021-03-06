// -*- java -*-

package eem.frame.wave;

import eem.frame.core.*;
import eem.frame.event.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import robocode.Bullet;
import robocode.*;

import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.*;

public class  wavesManager {
	public CoreBot myBot;
	public LinkedList<wave> Waves = new LinkedList<wave>();
	public HashCounter<String> waveCount = new HashCounter<String>();

	public LinkedList<waveListener> waveListeners = new LinkedList<waveListener>();

	public wavesManager(CoreBot bot) {
		myBot = bot;
	}

	public void initTic(long timeNow) {
		profiler.start("wavesManager.initTic", !profiler.SHOW_CHILDREN);
		LinkedList<InfoBot> listOfAliveBots = myBot._gameinfo._botsmanager.listOfAliveBots();
		remove ( getListOfPassedWaves( listOfAliveBots, timeNow ) );
		checkForPassingOverBotsWaves( listOfAliveBots, timeNow );
		profiler.stop("wavesManager.initTic");
	}

	public void checkForPassingOverBotsWaves( LinkedList<InfoBot> listOfAliveBots, long timeNow ) {
		profiler.start("checkForPassingOverBotsWaves");
		for ( wave w: Waves ) {
			for ( InfoBot bot : listOfAliveBots ) {
				if ( w.isPassingOverBot( bot, timeNow ) ) {
					myBot._gameinfo.onWavePassingOverBot( w, bot );	
				}
			}
		}
		profiler.stop("checkForPassingOverBotsWaves");
	}

	public void add( wave w )  {
		Waves.add( w );
		waveCount.incrHashCounter( w.firedBot.getName() );
		w.setCount( waveCount.getHashCounter(w.firedBot.getName() ) );
		//logger.dbg( "bot " + w.firedBot.getName() + " fired wave " + w.getCount() );
		for ( waveListener l : waveListeners ) {
			l.waveAdded(w);
		}
	}

	public void remove(wave w) {
		Waves.remove( w );
		for ( waveListener l : waveListeners ) {
			l.waveRemoved(w);
		}
	}

	public void remove(LinkedList<wave> wavesToRemove) {
		profiler.start("remove");
		for ( wave w : wavesToRemove ) {
			remove( w );
		}
		profiler.stop("remove");
	}

	public LinkedList<wave> getWavesOfBot( fighterBot bot ) {
		LinkedList<wave> botWaves = new LinkedList<wave>();
		for (wave w: Waves ) {
			if ( w.firedBot.getName().equals( bot.getName() ) ) {
				botWaves.add(w);
			}
		}
		return botWaves;
	}

	public LinkedList<wave> getListOfPassedWaves(LinkedList<InfoBot> listOfAliveBots, long timeNow) {
		profiler.start("getListOfPassedWaves");
		LinkedList<wave> passedWaves = new LinkedList<wave>();
		ListIterator<wave> wLIter;
		wLIter = Waves.listIterator();
		while (wLIter.hasNext()) {
			wave w = wLIter.next();
			boolean isWaveActive = false;
			for ( InfoBot bot : listOfAliveBots ) {
				if ( !w.isBehindBot( bot, timeNow ) ) {
					// the wave is still active
					isWaveActive = true;
					break;
				}
			}
			if ( !isWaveActive ) {
				//wLIter.remove();
				passedWaves.add( w );
			}
		}
		profiler.stop("getListOfPassedWaves");
		return( passedWaves );
	}

	public void addWaveListener(waveListener l) {
		waveListeners.add(l);
	}

	public wave getWaveMatching( HitByBulletEvent e ) {
		Bullet b = e.getBullet();
		Point2D.Double posHit = new Point2D.Double( b.getX(), b.getY() );
		String firedBotName = e.getName();
		String trgtBotName = myBot.getName();
		InfoBot trgtBot = myBot.getGameInfo().getFighterBot( trgtBotName ).getInfoBot();
		long timeNow = myBot.getTime();
		return  getWaveMatching( firedBotName, trgtBotName, posHit, timeNow );
	}

	public wave getWaveMatching( String firedBotName, String trgtBotName, Point2D.Double posHit, long timeNow ) {
		wave wRet = null;
		double minDist = 1e6; // crazy large
		for( wave w: Waves ) {
			if ( firedBotName.equals( w.firedBot.getName() ) ) {
				// the wave belong to the event firedBot
				// now we need to see if it the wave which hit
				double distTraveled = w.getDistanceTraveledAtTime( timeNow );
				double distToHitPos = Math.abs( distTraveled - posHit.distance( w.getFiredPosition() ) );
				// fixme: it's better to do bullet intersect since we know its direction
				// and the hit position
				if ( distToHitPos  <= w.getBulletSpeed() ) {
					if ( distToHitPos < minDist ) {
						// this is the wave which is over and thus hit the bot
						wRet = w;
						minDist = distToHitPos;
					}
				}
			}
		}
		return wRet;
	}

	public void onPaint(Graphics2D g) {
		long timeNow = myBot.getTime();
		for ( wave w : Waves ) {
			w.onPaint(g, timeNow);
		}
	}
}

