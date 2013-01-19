package brunoPlayer2;

import java.util.ArrayList;

import battlecode.common.*;

public class RobotPlayer {
	private static RobotController rc;
	// Soldier variables
	private static boolean assigned = false;
	private static int groupFrequency;
	private static int mission = 1;
	private static MapLocation goal;
	// HQ variables
	private static int groupSize = 10;
	private static int maxSoldiers = 40;
	private static MapLocation[]encampmentLocations;
	private static MapLocation[] mineLocations;
	private static ArrayList<Group> groupsList;
	private static ArrayList<Integer> newSoldiers;
	private static Upgrade[] upgrades = {Upgrade.VISION, Upgrade.FUSION, Upgrade.DEFUSION, Upgrade.PICKAXE, Upgrade.NUKE};
	private static int currentUpgrade = 0;
	private static int upgradeProgress = 0;
	private static MapLocation rallyPoint;
	private static int size = 0;
	private static int scout = 0;
	// Constants
	private static int criticalRangeSquared = 32; // TODO optimize
	private static int criticalHealth = 10; // TODO optimize
	private static int rallyRange = 10; // TODO optimize
	public static boolean retreat = false;
	
	public static void run (RobotController myRC) throws GameActionException{
		rc = myRC;
		rallyPoint = findRallyPoint();
		while(true){
			switch(rc.getType()){
			case ARTILLERY:
				artilleryCode();
				break;
			case GENERATOR:
				generatorCode();
				break;
			case HQ:
				hqCode();
				break;
			case MEDBAY:
				medbayCode();
				break;
			case SHIELDS:
				shieldsCode();
				break;
			case SOLDIER:
				soldierCode();
				break;
			case SUPPLIER:
				supplierCode();
				break;
			default:
				break;
			
			}
		}
	}

	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/5;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}

	private static void artilleryCode() {
		checkOutnumbered();
		rc.yield();
	}

	private static void generatorCode() {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void hqCode() throws GameActionException {
		// Populate list of encampments to be captured
		if(Clock.getRoundNum() == 0){
			
			encampmentLocations = rc.senseAllEncampmentSquares();
			mineLocations = rc.senseMineLocations(new MapLocation(rc.getMapHeight()/2, rc.getMapWidth()/2), 100, Team.NEUTRAL);
			
		}
		
		if (Clock.getRoundNum() == 170) retreat = true;
		if (Clock.getRoundNum() == 220) retreat = false;
		if (Clock.getRoundNum() == 600) retreat = true;
		if (Clock.getRoundNum() == 1200) retreat = false;
		
		
		groupSize = determineGroupSize();
		maxSoldiers = calculateMaxSoldiers();
		// Spawn troops if empty space around the base exists
		// Spawn troops if there is power to spare
		if (countSoldiers() < maxSoldiers){
			for (Direction d:Direction.values()){
				if (rc.canMove(d)){
					try {
						rc.spawn(d);
					} catch (GameActionException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		// If there is no power to spare for troops, research upgrades
		}
		else{
			researchUpgrades();
		}
		assignMissions();
		manageDefenses();
		manageEncampments();
		rc.yield();
	}
	

	private static void medbayCode() {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void shieldsCode() {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void soldierCode() throws GameActionException {
		/*if (!assigned){
			joinGroup();
		}
		checkOutnumbered();
		checkEncampment();
		int [] costs = new int[8];
		if (rc.getEnergon() < criticalHealth){
			costs = retreatCalculate();
		}
		else if (rc.senseNearbyGameObjects(Robot.class,criticalRangeSquared,rc.getTeam().opponent()).length > 0){
			costs = interceptCalculate();
		}
		else{
			switch(mission){
			case 1:
				costs = rallyCalculate();
				break;
			case 2:
				costs = attackCalculate();
				break;
			case 3:
				costs = captureCalculate();
				break;
			case 4:
				costs = defendCalculate();
				break;
			default:
				break;
			}
		}
		Direction dir = findMin(costs);
		try{
			if (true) // TODO replace by if there is no mine in the direction and the unit can move
			{rc.move(dir);}
			else if (true) // TODO replace by if there is a mine in the direction
			{} // defuse
			else // if the unit cannot move in that direction
			{} // stay still //TODO implement a time-out system if the robot stays still for too long
		}
		catch (GameActionException e) {
			e.printStackTrace();
		}*/
		
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
		if (rc.isActive()){
			Direction next;
			if(enemyRobots.length==0){//no enemies nearby
				
				if (Clock.getRoundNum()<160){
					next = nextDirection(rallyPoint);
					if (rc.senseMine(rc.getLocation().add(next))!=null) 
					{
					if (rc.isActive()) rc.defuseMine(rc.getLocation().add(next));
					rc.yield();
					}
					else if (rc.senseEncampmentSquare(rc.getLocation()))  {
						if (rc.isActive()) rc.captureEncampment(RobotType.MEDBAY);
						rc.yield();
					}
					/*else if (rc.senseMine(rc.getLocation().add(Direction.NONE))==null && !rc.getLocation().isAdjacentTo(rc.senseHQLocation())) {
						if (rc.isActive()) rc.layMine();
						rc.yield();
					}*/
					else {
						if (next!=Direction.NONE && rc.isActive() && rc.canMove(next)) rc.move(next);
						rc.yield();
					}
					
				}
				else{
					if (retreat==false) {
						next = nextDirection(rc.senseEnemyHQLocation());
						if (rc.senseMine(rc.getLocation().add(next))!=null) 
						{
						if (rc.isActive()) rc.defuseMine(rc.getLocation().add(next));
						rc.yield();
						}
						else {
							if (next!= Direction.NONE && rc.isActive() && rc.canMove(next)) rc.move(next);
							rc.yield();
						}
						/*next = minelessDirection(rc.senseEnemyHQLocation());
						if (next!= Direction.NONE && rc.isActive() && rc.canMove(next)) rc.move(next);
						rc.yield();*/
					}
					else {
						next = nextDirection(rc.senseAlliedEncampmentSquares()[0]);
						if (rc.senseMine(rc.getLocation().add(next))!=null) 
						{
						if (rc.isActive()) rc.defuseMine(rc.getLocation().add(next));
						rc.yield();
						}
						else {
							if (next!= Direction.NONE && rc.isActive() && rc.canMove(next)) rc.move(next);
							rc.yield();
						}
						/*next = minelessDirection(rc.senseEnemyHQLocation());
						if (next!= Direction.NONE && rc.isActive() && rc.canMove(next)) rc.move(next);
						rc.yield();*/
					}
					
					
				}
				}
			else{//someone spotted
				int closestDist = 1000000;
				MapLocation closestEnemy=null;
				for (int i=0;i<enemyRobots.length;i++){
					Robot arobot = enemyRobots[i];
					RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
					int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
					if (dist<closestDist){
						closestDist = dist;
						closestEnemy = arobotInfo.location;
					}
				}
				//next = minelessDirection(closestEnemy);
				
				next = nextDirection(closestEnemy);
				if (rc.senseMine(rc.getLocation().add(next))!=null) 
				{
				if (rc.isActive()) rc.defuseMine(rc.getLocation().add(next));
				rc.yield();
				}
				if (next!=Direction.NONE && rc.isActive() && rc.canMove(next)) rc.move(next);
				rc.yield();
			}
			}
		rc.yield();

	}
		

	private static Direction nextDirection(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			int[] directionOffsets = {0,1,-1,2,-2};
			Direction lookingAtCurrently = dir;
			lookAround: for (int d:directionOffsets){
				lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(lookingAtCurrently)){
					break lookAround;
				}
			}
			return lookingAtCurrently;
		}
		return Direction.NONE;
	}
	private static Direction minelessDirection (MapLocation whereToGo) throws GameActionException {
		Direction lookingAtCurrently;
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			int[] directionOffsets = {0,1,-1,2,-2};
			lookingAtCurrently = dir;
			for (int d:directionOffsets){
				lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if (rc.canMove(lookingAtCurrently) && rc.senseMine(rc.getLocation().add(lookingAtCurrently))==null) {
					break;
				}
			}
			return lookingAtCurrently;
		}
		return Direction.NONE;
	}

	private static void supplierCode() {
		checkOutnumbered();
		rc.yield();
	}
	
	/**
	 * For use by all except HQ.
	 * 
	 * Check whether the number of enemies within sight range is
	 * greater than the number of friendly units.
	 * If so, message HQ.
	 */
	private static void checkOutnumbered() {
		//TODO implement
	}
	
	/**
	 * For use by soldier.
	 * 
	 * Check whether the soldier is standing on an encampment and not capturing it.
	 * Message HQ and wait for instructions regarding what to build.
	 */
	private static void checkEncampment() {
		//TODO implement
	}
	
	/**
	 * For use by soldier.
	 * 
	 * Read channel corresponding to soldier ID for group channel assignment.
	 * Set assigned to true if a group assignment is obtained and clear the channel.
	 * Send message to HQ on channel frequency matching ID, signifying that
	 * soldier is ready for action, if the read does not result in an assignment.
	 */
	private static void joinGroup() {
		//TODO implement
	}
	
	/**
	 * All calculate methods for use by soldier.
	 * 
	 * Depending on the type of movement required, calculate a heuristic value
	 * for the 8 squares surrounding the soldier. Return in an array of length
	 * 8, in the order that the directions are declared in the enum.
	 * Use a for loop over Direction.values().
	 * 
	 * @return an array of integer costs
	 */
	private static int[] retreatCalculate() {
		//TODO implement
		return null;
	}
	private static int[] interceptCalculate() {
		//TODO implement
		return null;
	}
	private static int[] rallyCalculate() {
		//TODO implement
		return null;
	}
	private static int[] attackCalculate() {
		//TODO implement
		return null;
	}
	private static int[] captureCalculate() {
		//TODO implement
		return null;
	}
	private static int[] defendCalculate() {
		//TODO implement
		return null;
	}
	
	/**
	 * For use by soldier.
	 * 
	 * @param costs array of costs of all surrounding squares
	 * @return the direction of least cost
	 */
	private static Direction findMin(int[] costs) {
		//TODO implement
		return null;
	}
	
	/**
	 * For use by HQ.
	 * 
	 * Determine how large the attack groups should be (exploration groups have size 1 always)
	 * based on how often we are getting outnumbered.
	 * 
	 * @return the new group size
	 */
	private static int determineGroupSize() {
		//TODO implement
		return groupSize;
	}
	
	/**
	 * For use by HQ.
	 * 
	 * Calculate the maximum number of troops that we can support based on the
	 * number of generators and known bytecode costs of soldiers.
	 * 
	 * @return the new maximum number of soldiers
	 */
	private static int calculateMaxSoldiers() {
		//TODO implement
		return maxSoldiers;
	}
	
	/**
	 * For use by HQ.
	 * 
	 * Read broadcasts from soldiers reporting for duty.
	 * Maintain list of soldiers that are ready.
	 * Determine which missions need to be completed.
	 * Message soldiers and group channels accordingly.
	 */
	private static void assignMissions() {
		//TODO implement
	}
	
	/**
	 * For use by HQ.
	 * 
	 * Listen for soldiers reporting that they have reached encampments.
	 * Determine what types of encampments are needed.
	 * Message group channels accordingly.
	 */
	private static void manageEncampments() {
		
	}
	
	/**
	 * For use by HQ.
	 * 
	 * Listen for soldiers and encampments reporting that they are outnumbered.
	 * Determine how to respond.
	 * Message group channels and unassigned soldiers accordingly.
	 */
	private static void defendHQ() {
		
	}
	private static void manageDefenses() {

	}
	
	/**
	 * For use by HQ.
	 * 
	 * Determine priority order for upgrades and research.
	 * @throws GameActionException 
	 */
	private static void researchUpgrades() throws GameActionException {
		if (currentUpgrade<4){
			rc.researchUpgrade(upgrades[currentUpgrade]);
			upgradeProgress++;
			if (upgradeProgress == 24) {
				currentUpgrade++;
				upgradeProgress = 0;
			}
		}
		
	}
	
	/**
	 * 
	 * @return the total number of soldiers on our team that exist at a given time
	 */
	private static int countSoldiers() {
		return rc.senseNearbyGameObjects(Robot.class,(Math.max(rc.getMapHeight(),rc.getMapWidth()))^2,Team.A).length;
		
	}
	
	private static void attackMicro() {
		
	}
}
