// -*- java -*-

package eem.frame.gun;

import eem.frame.gun.*;
import eem.frame.wave.*;
import eem.frame.bot.*;
import eem.frame.misc.*;

import eem.frame.external.trees.secondGenKD.KdTree;

import java.util.LinkedList;
import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;

import robocode.*;
import robocode.util.*;
import robocode.Rules.*;


public class gunManager implements gunManagerInterface {
	public HashMap<String, LinkedList<baseGun>> gunListForGameType = new HashMap<String, LinkedList<baseGun>>();
	public LinkedList<baseGun> gunList = new LinkedList<baseGun>(); // this one assigned from above
	public fighterBot myBot;
	fighterBot targetBot = null;
	protected int  firedCount = 0;
	double firingSolutionQualityThreshold = .000;
	long fireAtTime = -9999; // gun manager will set this time
	LinkedList<firingSolution> firingSolutions = new LinkedList<firingSolution>();
	firingSolution bestFiringSolution = null;

	public HashCounter<String> hitByOther = new HashCounter<String>();
	public HashCounter<String> hitByMe = new HashCounter<String>();
	public HashCounter<String> firedAt = new HashCounter<String>();
	public HashCounter<String> firedByEnemy = new HashCounter<String>();
	public HashCounter<String> hitBullet = new HashCounter<String>();

	// my gun stats
	public HashCounter<String2D> hitByMyGun = new HashCounter<String2D>();
	public HashCounter<String2D> hitByEnemyGun = new HashCounter<String2D>();
	public HashCounter<String2D> firedAtEnemyByGun = new HashCounter<String2D>();

	protected int numGuessFactorBins = 31;
	protected HashMap<String, double[]> guessFactorsMap = new HashMap<String, double[]>();
	protected double decayRate = .8;
	protected HashMap<String, double[]> decayingGuessFactorMap = new HashMap<String, double[]>();
	protected HashMap<String, double[][]> assistedGFactorsMap = new HashMap<String, double[][]>();

	protected int kdTreeSizeLimit = 10000;
	protected HashMap<String, KdTree<gfHit>> guessFactorsKDTreeMap = new HashMap<String, KdTree<gfHit>>();
	protected HashMap<String, KdTree<gfHit>> realHitsGFKDTreeMap = new HashMap<String, KdTree<gfHit>>();
	int hitProbEstimateNeighborsNum = 100;
	protected HashMap<String2D, KdTree<gunHitMissLog>> gunHitMissKDTreeMap = new HashMap<String2D, KdTree<gunHitMissLog>>();
	protected HashMap<aimingConditions, List<KdTree.Entry<gfHit>> > kdClusterCache = new HashMap<aimingConditions, List<KdTree.Entry<gfHit>> >();
	protected HashMap<aimingConditions, double[] > cachedMEAs = new HashMap<aimingConditions, double[] >();

	public	gunManager() {
	}

	public	gunManager(fighterBot bot) {
		this();
		myBot = bot;
	}

	public	void setGunsMap(HashMap<String, LinkedList<baseGun>> gMap) {
		gunListForGameType = gMap;
	}

	public LinkedList<baseGun> getGunList() {
		return gunList;
	}

	public void initTic() {
		String fightType = myBot.getGameInfo().fightType();
		gunList = gunListForGameType.get( fightType );
		if ( myBot.proxy.getGunHeat()/physics.gunCoolingRate >  (180/robocode.Rules.GUN_TURN_RATE + 1) ) {
			// trying to save CPU cycles, when gun is hot
			// fixme: be smarter about it, may be use MEA
			//logger.dbg("Tic: " + myBot.getTime() + " gunHeat = " + myBot.proxy.getGunHeat() + " aiming only HeadOnGun gun");
			gunList = new LinkedList<baseGun>();
			gunList.add( new headOnGun() );
		}
		if ( gunList == null ) {
			logger.dbg("no gun list for the fight type: " + fightType  + ", choosing default");
			gunList = gunListForGameType.get( "default" );
		}
		if ( gunList == null ) {
			logger.error("ERROR: no gun list for the default fight type. Assigning empty list");
			gunList = new LinkedList<baseGun>();
		}
	}

	public void initBattle() {
	}

	public void manage() {
	}

	public HashMap<aimingConditions, List<KdTree.Entry<gfHit>> > getKdClusterCache() {
		return kdClusterCache;
	}

	public HashMap<aimingConditions, double[] > getMEAsCache() {
		return cachedMEAs;
	}

	public void logHitOrMissForMyFS( firingSolution fS ) {
		String gunName = fS.getGunName();
		String  enemyName = fS.getTargetBotName();
		String2D key = new String2D( gunName, enemyName );
		incrFiredAtEnemyByGun( fS );
		gunHitMissLog hmLog = new gunHitMissLog(false, 1);
		if ( !fS.isActive() ) {
			hitByMyGun.incrHashCounter( key );
			hmLog.hitStat = true;
			//logger.dbg(myBot.getName() + " hit enemy: " + fS.getTargetBotName() + " with gun: " + fS.getGunName() + " fired at dist: " + fS.getDistanceAtLastAim() );
		} else {
			//logger.dbg(myBot.getName() + " missed enemy: " + fS.getTargetBotName() + " with gun: " + fS.getGunName() + " fired at dist: " + fS.getDistanceAtLastAim() );
		}
		gunTreePoint gTP = new gunTreePoint(
				myBot,
				myBot.getGameInfo().getFighterBot( enemyName ).getInfoBot(),
				fS.getFiredTime(), fS.getBulletEnergy()
				);
		KdTree<gunHitMissLog> tree = getGunHitMissKDTree( key );
		tree.addPoint( gTP.getPosition(), hmLog );
	}

	public void addRealHitGF( double gf,  InfoBot tBot, long firedTime, double bulletEnergy ) {
		logger.dbg(myBot.getTime() + ": addRealHitGF to " + myBot.getName() + " for target " + tBot.getName() + " at GF = " + gf + " with bullet energy " + bulletEnergy );
		String gunName = "realHitsGun";
		String trgtBotName = tBot.getName();
		String2D key = new String2D( gunName, trgtBotName );

		KdTree<gfHit> tree = getRealHitsGFKDTreeMap( trgtBotName );
		gunTreePoint gTP = new gunTreePoint( myBot, tBot, firedTime, bulletEnergy );
		double [] pntCoord =  gTP.getPosition();
		int i = (int)math.gf2bin( gf, numGuessFactorBins );
		double binW = 1.0;
		gfHit gfH = null;
		gfH = new gfHit(i, binW);
		gfH.firedTime = firedTime;
		tree.addPoint( pntCoord, gfH );

		// this uses game symmetry
		int iFlipped = (int)math.gf2bin( -gf, numGuessFactorBins );
		gfH = new gfHit(iFlipped, binW);
		gfH.firedTime = firedTime;
		tree.addPoint( gTP.calcFlipedLateralVelocityPositionFromCoord(pntCoord), gfH ); // shall I decrease binW?

		// now we update all flying waves with realHit new dangers
		// this should help master bot to shift from just hit GF
		// otherwise this info will be used only in newly fired waves
		// which is too late, since enemy tends to fire at the same GF
		// for a while
		// FIXME non master bot do not track their waves!
		// thus I comment out below
		//for (waveWithBullets wB : myBot.myWaves ) {
		fighterBot masterBot = myBot.getGameInfo().getFighterBot(myBot.getGameInfo().getMasterBot().getName());
		for (waveWithBullets wB : masterBot.getEnemyWaves() ) {
			if ( wB.getFiredBot().getName().equals( myBot.getName() ) ) {
				// this wave belong to this bot
				double[] gfA = calcMyWaveGFdanger( wB.getTargetBot(), wB.getFiredTime(), wB.getBulletEnergy() );
				wB.copyGFarray( gfA );
				wB.calcCombineDanger();
			}
		}
	}

	// someone hit the master bot
	public void onHitByBullet(HitByBulletEvent e) {
		// target is actually master bot
		String trgtBotName = myBot.getGameInfo().getMasterBot().getName();
		String fireBotName = e.getName();

		incrHitCounts( trgtBotName, fireBotName );
		if ( fireBotName.equals( myBot.getName() ) ) {
			// this is the bot which send the bullet to hit the masterBot
			wave w = myBot.getGameInfo().getWavesManager().getWaveMatching( e );
			if ( w == null) {
				logger.dbg( "cannot locate the wave matching HitByBulletEvent");
				return;
			} else {
				// this bot waves are in the list of the master bot enemy waves
				// FIXME: enemy bot must have its own list of fired waves
				// otherwise selection is not efficient since we selecting
				// other bots waves too
				LinkedList<waveWithBullets> myWaves = myBot.getGameInfo().getFighterBot( trgtBotName ).getEnemyWaves();
				// which of my waves with bullet it is
				for (waveWithBullets wB : myWaves ) {
					if ( wB.equals( w ) ) {
						String gunName;
						String2D key;
						Bullet b = e.getBullet();
						Point2D.Double posHit = new Point2D.Double( b.getX(), b.getY() );
						// registering real hit
						double gf = wB.getPointGF( posHit );
						//logger.dbg(myBot.getTime() + ": " + fireBotName + " hit " + trgtBotName + " at GF = " +gf + " with real bullet");
						if ( gf < -1 || gf > 1) {
							logger.error("error: GF out of range. It should be with in -1..1 but we got " + gf);
						}
						addRealHitGF( gf,  myBot.getGameInfo().getFighterBot( trgtBotName ).getInfoBot(),  w.getFiredTime(), w.getBulletEnergy() );
						// FIXME: it would be good idea to update all danger GF for already flying waves of this bot towards trgtBotName according to this new information

						LinkedList<firingSolution> fSwhichHit =  wB.getFiringSolutionsWhichHitBotAt( posHit, myBot.getTime() );
						if ( fSwhichHit.size() == 0 ) {
							gunName = "unknownGun";
							//logger.dbg("masterBot is hit by " + gunName + " from bot " + fireBotName );
							key = new String2D( gunName, trgtBotName );
							hitByMyGun.incrHashCounter( key );
						} else {
							for( firingSolution fS : fSwhichHit ) {
								gunName = fS.getGunName();
								//logger.dbg("masterBot is hit by " + gunName + " from bot " + fireBotName );
								key = new String2D( gunName, trgtBotName );
								hitByMyGun.incrHashCounter( key );
								logger.dbg(myBot.getName() + " hit enemy: " + fS.getTargetBotName() + " with gun: " + fS.getGunName() + " fired at dist: " + fS.getDistanceAtLastAim() );
							}
						}

						break;
					}
				}
			}
		}
	}

	public double[] calcMyWaveGFdanger( fighterBot tBot, long firedTime, double bulletEnergy ) {
		// set GF array with real hits gf
		kdtreeGuessFactorGun g = new realHitsGun(10);
		//kdtreeGuessFactorGun g = new kdtreeGuessFactorGun(50); // flattener?
		//kdtreeGuessFactorGun g = new kdtreeGuessFactorGun(50,true); // anti-flattener?
		LinkedList<firingSolution> gunfSols =  g.getFiringSolutions( myBot, tBot.getInfoBot(), firedTime, bulletEnergy ); // this is a dummy but it sets tree point coord
		double[] gfA=g.getGFdanger( myBot, tBot.getInfoBot() );
		//logger.dbg(logger.arrayToTextPlot( gfA ) + " GF danger" );
		return gfA;
	}

	public fighterBot getTarget() {
		return targetBot;
	}

	public fighterBot getClosestTarget() {
		double dist2closestBot = 1e6; // something very large
		long infoDelayTimeThreshold = (long) (360/robocode.Rules.RADAR_TURN_RATE + 1);
		double distNew;
		for ( fighterBot eBot: myBot.getEnemyBots() ) {
			distNew = myBot.getPosition().distance( eBot.getPosition() );
			if ( ( distNew < dist2closestBot) && ((myBot.getTime() - eBot.getLastSeenTime()) < infoDelayTimeThreshold) ) {
				dist2closestBot = distNew;
				targetBot = eBot;
			}
		}
		return targetBot;
	}

	// master bot bullet hit someone
	public void  onBulletHit(BulletHitEvent e) {
		String trgtBotName = e.getName();
		String fireBotName = myBot.getGameInfo().getMasterBot().getName();
		incrHitCounts( trgtBotName, fireBotName );
		Bullet b = e.getBullet();
		long time = myBot.getTime();
		Point2D.Double posHit = new Point2D.Double( b.getX(), b.getY() );
		InfoBot trgtBot = myBot.getGameInfo().getFighterBot( trgtBotName ).getInfoBot();
		//see  http://robowiki.net/wiki/Talk:Waves/Precise_Intersection#Smashing_it_down_to_bins
		Point2D.Double posBot = trgtBot.getPositionAtTime( time -1 );  // note time-1
		if ( posBot == null ) // this is likely in melee
			posBot = trgtBot.getPositionAtTime( time );
		if ( posBot == null ) // this is likely in melee
			posBot = posHit; // best guess

		wave w = myBot.getGameInfo().getWavesManager().getWaveMatching( fireBotName, trgtBotName, posHit, time );
		if ( w == null) {
			logger.error("error: cannot match a BulletHitEvent to my own wave");
			return;
		}
		for ( waveWithBullets wB: myBot.myWaves ) {
			if ( w.equals( wB) ) {
				wB.setMyWavePassedOverTargetFlag( trgtBotName, true );
				wB.markFiringSolutionWhichHitBotAt( posBot, trgtBotName, time);
			}
		}
	}
	
	// our bullet hit a bullet from another bot
	public void onBulletHitBullet(fighterBot eBot) {
		hitBullet.incrHashCounter( eBot.getName() );
	}

	// checks if enemy employs bullet shield
	public boolean isBulletShieldDetected( String eName ) {
		int fC = firedByEnemy.getHashCounter( eName );
		double erate = math.eventRate( hitBullet.getHashCounter( eName ), fC );
		int confedenceNum = 10;
		double detectionTshreshold = 0.2;
		if ( (fC >= confedenceNum) && ( erate >= detectionTshreshold ) ) {
			return true;
		}
		return false;
	}

	public void incrHitCounts( String trgtBotName, String fireBotName ) {
		if ( myBot.getName().equals( trgtBotName ) ) {
			// this bot is hit by other
			hitByOther.incrHashCounter( fireBotName );
		}
		if ( myBot.getName().equals( fireBotName ) ) {
			// this bot hit someone
			hitByMe.incrHashCounter( trgtBotName );
		}
	}

	public void reportHitByOther(){
		logger.routine("Hit me count by the following bot(s)");
		for ( fighterBot fB: myBot.getAllKnownEnemyBots() ) {
			String bName = fB.getName();
			logger.routine( " " + bName + ": " + logger.hitRateFormat( hitByOther.getHashCounter( bName ) , firedByEnemy.getHashCounter( bName ) ) );
		}
	}

	public void onMyWavePassingOverBot( wave w, InfoBot bot ) {
		// FIXME: update stats
		// FIXME: firing solution need to know target otherwise
		//        we count bullets directed to someone else
		long time = myBot.getTime();
		String  enemyName = bot.getName();
		// and http://robowiki.net/wiki/Talk:Waves/Precise_Intersection#Smashing_it_down_to_bins
		// It boils down to: Each tick, the bullet moves forward, forming a line segment,
		// and it is checked if this line segment intersects the bot bounding box,
		// then the enemy bot moves.
		Point2D.Double botPos = bot.getPositionClosestToTime( time - 1 );

		updateHitGuessFactor( bot, w );

		// count the wave with bullets
		for ( waveWithBullets wB: myBot.myWaves ) {
			if ( w.equals( wB) ) {
				wB.setMyWavePassedOverTargetFlag( enemyName, true );
				wB.markFiringSolutionWhichHitBotAt( botPos, enemyName, time);
			}
		}
	}

	public double[] getGuessFactors( String  botName ) {
		return getGuessFromHashMap( guessFactorsMap, botName) ;
	}

	public double[] getDecayingGuessFactors( String  botName ) {
		return getGuessFromHashMap( decayingGuessFactorMap, botName) ;
	}

	public double[] getGuessFromHashMap( HashMap<String, double[]> map, String  botName ) {
                if ( !map.containsKey( botName ) ) {
			double[] guessFactorBins = new double[numGuessFactorBins];
			map.put( botName, guessFactorBins );
                }
                double[] gfBins = map.get( botName );
		return gfBins;
	}

	public double[][] getAssistedGuess( String  botName ) {
		return getAssistedGuessFromHashMap( assistedGFactorsMap, botName);
	}

	public double[][] getAssistedGuessFromHashMap( HashMap<String, double[][]> map, String  botName ) {
                if ( !map.containsKey( botName ) ) {
			double[][] guessFactorBins = new double[numGuessFactorBins][numGuessFactorBins];
			map.put( botName, guessFactorBins );
                }
                double[][] gfBins = map.get( botName );
		return gfBins;
	}

	public KdTree<gfHit> getTreeKDTreeMap( String  botName ) {
                if ( !guessFactorsKDTreeMap.containsKey( botName ) ) {
			gunTreePoint gTP = new gunTreePoint();
			KdTree<gfHit> tree = new KdTree.Manhattan<gfHit>( gTP.getKdTreeDims(), kdTreeSizeLimit );
			guessFactorsKDTreeMap.put( botName, tree );
                }
		KdTree<gfHit> tree = guessFactorsKDTreeMap.get( botName );
		return tree;
	}

	public KdTree<gfHit> getRealHitsGFKDTreeMap( String  botName ) {
                if ( !realHitsGFKDTreeMap.containsKey( botName ) ) {
			gunTreePoint gTP = new gunTreePoint();
			KdTree<gfHit> tree = new KdTree.Manhattan<gfHit>( gTP.getKdTreeDims(), kdTreeSizeLimit );
			realHitsGFKDTreeMap.put( botName, tree );
                }
		KdTree<gfHit> tree = realHitsGFKDTreeMap.get( botName );
		return tree;
	}


	public KdTree<gunHitMissLog> getGunHitMissKDTree( String2D  key ) {
                if ( !gunHitMissKDTreeMap.containsKey( key ) ) {
			gunTreePoint gTP = new gunTreePoint();
			KdTree<gunHitMissLog> tree = new KdTree.Manhattan<gunHitMissLog>( gTP.getKdTreeDims(), kdTreeSizeLimit );
			gunHitMissKDTreeMap.put( key, tree );
                }
		KdTree<gunHitMissLog> tree = gunHitMissKDTreeMap.get( key );
		return tree;
	}

	public int getGuessFactosrBinNum() {
		return numGuessFactorBins;
	}

	public void updateHitGuessFactor( InfoBot bot, wave w ) {
		// calculate myBot targeting GF
		long time = myBot.getTime();
		double gf = w.getFiringGuessFactor(bot, time);
		double gfRange = w.getFiringGuessFactorRange( bot, time );
		double distAtLastAim = w.getDistanceAtLastAimTime( bot );
		// get circular gun guess factor
		double circularGF = Double.NaN;
		circularGun circGun = new circularAccelGun();
		LinkedList<firingSolution> fSs = circGun.getFiringSolutions( w.getFiredPosition(), bot, w.getFiredTime(), w.getBulletEnergy() );
		if ( fSs.size() >= 1 ) {
			double cAngle = fSs.getFirst().getFiringAngle();
			circularGF = w.getFiringGuessFactor( bot, cAngle );
		}

		//logger.routine("hitGF" +  " target:" + bot.getName() + " gf:" + gf + " cgf:" +circularGF + " distance:" + distAtLastAim );
		int di0 = (int)Math.round( gfRange/2*numGuessFactorBins );
		int iCenter = (int)math.gf2bin( gf, numGuessFactorBins );

		KdTree<gfHit> tree = getTreeKDTreeMap( bot.getName() );
		gunTreePoint gTP = new gunTreePoint( myBot, bot, w.getFiredTime(), w.getBulletEnergy() );
		double [] pntCoord =  gTP.getPosition();

		double[] gfBins = getGuessFactors( bot.getName() );
		double[] gfBinsDecaying = getDecayingGuessFactors( bot.getName() );
		double[][] assistedGFBins = getAssistedGuessFromHashMap( assistedGFactorsMap, bot.getName() );
		// decay in all decaying map
		for ( int k=0; k< numGuessFactorBins; k++) {
			gfBinsDecaying[k] *= decayRate;	
		}

		//logger.dbg( "iCenter = " + iCenter + " di0 = " + di0 );
		// update guess factors tree
		double binW = 1;
		gfHit gfH = null;
		gfH = new gfHit(iCenter, binW);
		gfH.firedTime = w.getFiredTime();
		tree.addPoint( pntCoord, gfH );

		// this uses game symmetry
		int iFlipped = (int)math.gf2bin( -gf, numGuessFactorBins );
		gfH = new gfHit(iFlipped, binW);
		gfH.firedTime = w.getFiredTime();
		tree.addPoint( gTP.calcFlipedLateralVelocityPositionFromCoord(pntCoord), gfH ); // shall I decrease binW?

		int minI = (int)math.putWithinRange( iCenter - 2*di0, 0, (numGuessFactorBins-1) );
		int maxI = (int)math.putWithinRange( iCenter + 2*di0, 0, (numGuessFactorBins-1) );
		for ( int i = minI; i <= maxI; i++ ) {
			i = (int)math.putWithinRange( i, 0, (numGuessFactorBins-1) );

			double di = i-iCenter; // bin displacement from the center
			// every gf within (+/-)gfRange=di0 is a hit, so it should have
			// a weight close to 1. at 2*di0 we should have weight close to 1
			binW = Math.exp( - Math.pow( di/(1*di0) , 4 ) );

			// update accumulating map
			gfBins[i]+= binW;

			// update decaying map
			gfBinsDecaying[i] += (1-decayRate)*binW; // update bin where hit detected

			// update assisted GF map
			if ( !Double.isNaN( circularGF ) ) {
				int j = (int)math.gf2bin( circularGF, numGuessFactorBins );
				j = (int)math.putWithinRange( j, 0, (numGuessFactorBins-1) );
				assistedGFBins[j][i] += binW;
			}
		}
	}

	public void onWavePassingOverMe( wave w ) {
		long time = myBot.getTime();
		Point2D.Double botPos = myBot.getPosition( ); // time is now
		for ( waveWithBullets wB: myBot.enemyWaves ) {
			if ( w.equals( wB) ) {
				LinkedList<firingSolution> hitSolutions = wB.getFiringSolutionsWhichHitBotAt( botPos,  time );
				for ( firingSolution fS : hitSolutions ) {
					String gunName = fS.getGunName();
					String2D key = new String2D( gunName, wB.getFiredBot().getName() );
					if ( fS.isActive() ) {
						//logger.dbg(myBot.getName() + " was hit by " + gunName +" from " + wB.getFiredBot().getName() );
						hitByEnemyGun.incrHashCounter( key );
						//wB.removeFiringSolution( fS );
						fS.setActiveFlag( false );
					}
				}
			}
		}
	}

	public void reportHitByMe(){
		logger.routine("hit rate for the following bot(s) out of " + firedCount + " shots");
		for ( fighterBot fB: myBot.getAllKnownEnemyBots() ) {
			String bName = fB.getName();
			Integer fCnt;
			fCnt = firedAt.getHashCounter( bName );
			if ( fCnt == 0 ) {
				// we have no clue to whom we fired
				// using total firedCount
				fCnt = firedCount;
			}
			logger.routine( " " + bName + ": " + logger.hitRateFormat( hitByMe.getHashCounter( bName ), fCnt ) );
		}
	}

	public void reportMyGunStats() {
		String fightType = myBot.getGameInfo().fightType();
		String2D bestGunKey = new String2D();
		double bestHitRate = Double.NEGATIVE_INFINITY;
		ArrayList<Double> hitRateArray = new ArrayList<Double>();
	        gunList = gunListForGameType.get( fightType );
		// FIXME: some wave are still flying and are not fully accounted
		logger.routine("  My virtual gun hit rate stats stats");
		String str = "  ";
		// make headers
		str += String.format( "%25s", "enemy name" );
		for ( baseGun g: gunList ) {
			str += String.format( "%25s", g.getName() );
		}
		logger.routine( str );
		for ( fighterBot b: myBot.getAllKnownEnemyBots() ) {
			str = "  ";
			String enemyName = b.getName();
			str += String.format( "%25s", enemyName );
			for ( baseGun g: gunList ) {
				String2D key = new String2D( g.getName(), enemyName );
				str += String.format( "%25s", logger.hitRateFormat( hitByMyGun.getHashCounter( key ), firedAtEnemyByGun.getHashCounter( key ) ) );
				double hR = math.eventRate( hitByMyGun.getHashCounter( key ), firedAtEnemyByGun.getHashCounter( key ) );
				hitRateArray.add( hR) ;
				if  (hR > bestHitRate ) {
					bestHitRate = hR;
					bestGunKey = new String2D( g.getName(), enemyName );
				}

			}
			logger.routine( str );
			logger.routine( "Guns hit rates against " +  enemyName + " " + logger.arrayToTextPlot( hitRateArray.toArray(new Double[hitRateArray.size()] ) ) ); 
			logger.routine ("Best gun against " +  enemyName + ": " + bestGunKey.getX() + " with hit rate " + logger.hitRateFormat( hitByMyGun.getHashCounter( bestGunKey ), firedAtEnemyByGun.getHashCounter( bestGunKey ) ));
		}
	}

	public void reportEnemyGunStats() {
		// FIXME: some wave are still flying and are not fully accounted
		logger.routine("  Enemies virtual gun stats");
		for( String2D key: hitByEnemyGun.keySet() ) {
			logger.routine("    " + key.getX() + " of bot " + key.getY() + " hit me " + logger.hitRateFormat( hitByEnemyGun.getHashCounter( key ), firedByEnemy.getHashCounter( key.getY() ) ) );
		}
	}

	public void reportBulletHitBullet() {
		for( String key: hitBullet.keySet() ) {
			logger.routine( "bot " + key + " intercepted my bullet " + logger.hitRateFormat( hitBullet.getHashCounter( key ), firedByEnemy.getHashCounter( key ) ) );
		}
	}

	public void reportGFStats() {
		for( String key: guessFactorsMap.keySet() ) {
			//logger.routine( "bot " + key + " seen at          GF: " + Arrays.toString(guessFactorsMap.get(key)) );
			//logger.routine( "bot " + key + " seen at decaying GF: " + Arrays.toString(decayingGuessFactorMap.get(key)) );
		}
	}

	public void reportStats() {
		if ( myBot.isItMasterBotDriver() ) {
			reportHitByOther();
			reportEnemyGunStats();
		}
		reportHitByMe();
		reportGFStats();
		reportMyGunStats();
		if ( myBot.isItMasterBotDriver() ) {
			reportBulletHitBullet();
		}
	}

	public void incrFiredCount() {
		firedCount++;
	}

	public void incrFiredAtEnemyByGun(firingSolution fS) {
		String2D key = new String2D( fS.getGunName(), fS.getTargetBotName() );
		firedAtEnemyByGun.incrHashCounter( key );
	}

	public void incrFiredAtEnemyByGun(baseGun g, InfoBot eBot) {
		String2D key = new String2D( g.getName(), eBot.getName() );
		firedAtEnemyByGun.incrHashCounter( key );
	}

	public void incrFiredByEnemy(String enemyName) {
		firedByEnemy.incrHashCounter( enemyName );
	}

	public double getUnknownGunPerformanceAgainstBot( fighterBot targetBot ) {
		String2D keyUnknownGun = new String2D( "unknownGun", targetBot.getName() );
		double unkownGunPerfRate = math.perfRate( hitByMyGun.getHashCounter(keyUnknownGun) , firedAtEnemyByGun.getHashCounter(keyUnknownGun) );
		return unkownGunPerfRate;
	}

	public LinkedList<firingSolution> getFiringSolutions( fighterBot targetBot, long firingTime, double bulletEnergy ) {
		LinkedList<firingSolution> fSols = new LinkedList<firingSolution>();
		// generate solutions for each gun
		for ( baseGun g : gunList ) {
			// note getTime()+1, the fire command is executed at next tic
			LinkedList<firingSolution> gunfSols =  g.getFiringSolutions( myBot, targetBot.getInfoBot(), firingTime, bulletEnergy );
			String2D key = new String2D( g.getName(), targetBot.getName() );
			gunTreePoint gTP = new gunTreePoint(
					myBot,
					targetBot.getInfoBot(),
					firingTime, bulletEnergy
					);
			double [] treeCoord =  gTP.getPosition();
			KdTree<gunHitMissLog> tree = getGunHitMissKDTree( key );
			boolean isSequentialSorting = false;
			List<KdTree.Entry<gunHitMissLog>> cluster = tree.nearestNeighbor( treeCoord, hitProbEstimateNeighborsNum, isSequentialSorting );
			int fireCnt = 0;
			int hitCnt  = 0;
			for ( KdTree.Entry<gunHitMissLog> hmLog : cluster ) {
				fireCnt++;
				if ( hmLog.value.hitStat ) {
					hitCnt++;
				}
			}
			double gunHitKdTreeProb = math.perfRate( hitCnt, fireCnt );

			double gunPerfRate = math.perfRate( hitByMyGun.getHashCounter(key) , firedAtEnemyByGun.getHashCounter(key) );
			if ( myBot.isItMasterBotDriver() ) {
				// overwriting global gun performance
				gunPerfRate = gunHitKdTreeProb;
			}
			for ( firingSolution fS: gunfSols ) {
				double solQ = fS.getQualityOfSolution();
				fS.setQualityOfSolution( solQ * gunPerfRate );
				
			}
			fSols.addAll( gunfSols );
		}
		return fSols;
	}


	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
		if ( targetBot != null ) {
			double R = 50;
			graphics.drawCircle( g, targetBot.getPosition(), R );
			R++;
			graphics.drawCircle( g, targetBot.getPosition(), R );
			R++;
			graphics.drawCircle( g, targetBot.getPosition(), R );
		}
	}
}
