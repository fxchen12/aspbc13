package katiePlayer2;

import java.util.ArrayList;

import battlecode.common.*;


//Messaging code
/*
 * 1st-3rd digits) Three random digits set at top as constant randomMessagingDigits. Currently 152.
 * 4th digit) Digit corresponding to the type of message:
 * 		1 - group assignment
 * 		2 - report
 * 		3 - mission assignment
 * 		4 - arrived at encampment
 * 		5 - encampment type
 * 		6 - outnumbered
 * 5th-9th digits) Message (5 digits)
 * 		Group assignment: 5 digit number corresponding to a channel. 
 * 			If the number is less than 5 digits, then fill the early digits with 0s.
 * 			For example, channel 86 would be 00086
 * 		Report: All zeroes
 * 		Mission Assignment: 
 * 			a) Digit corresponding to mission type
 * 				1 - rally
 * 				2 - attack
 * 				3 - capture
 * 				4 - defend
 * 			b) Two digits corresponding to x coordinate of robot destination (example if only 1 digit: 5 becomes 05)
 * 			c) Two digits corresponding to y coordinate of robot destination (example if only 1 digit: 5 becomes 05)
 * 		Encampment type:
 * 			a) Digit corresponding to encampment type
 * 				1 - medbay
 * 				2 - shields
 * 				3 - artillery
 * 				4 - generator
 * 				5 - supplier
 * 			b) Other 4 digits are zeroes
 * 		Outnumbered
 * 			a) Digit corresponding to how significantly outnumbered a robot is (#enemies - #friends)
 * 			b) Two digits corresponding to x coordinate of robot position (example if only 1 digit: 5 becomes 05)
 * 			c) Two digits corresponding to y coordinate of robot position (example if only 1 digit: 5 becomes 05)
 * 
 * IMPORTANT: Soldier broadcasts go to channel = soldier ID + soldierBroadcastChannelOffset
 */


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
	private static ArrayList<Group> groupsList;
	private static ArrayList<Integer> newSoldiers;
	// Constants
	private static int criticalRangeSquared = 32; // TODO optimize
	private static int criticalHealth = 10; // TODO optimize
	private static int rallyRange = 10; // TODO optimize
	private static int randomMessagingDigits = 152; //random digits in front of every message
	private static int soldierBroadcastChannelOffset = 32100; //add this number to the soldier ID and use this to broadcast to HQ
	
	// Heuristic multipliers
	private static int directionMultiplier = 5;
	private static int mineMultiplier = 6;
	
	public static void run (RobotController myRC) throws GameActionException{
		rc = myRC;
		
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

	private static void artilleryCode() throws GameActionException {
		checkOutnumbered();
		rc.yield();
	}

	private static void generatorCode() throws GameActionException {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void hqCode() {
		// Populate list of encampments to be captured
		if(Clock.getRoundNum() == 0){
			encampmentLocations = rc.senseAllEncampmentSquares();
		}
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
	

	private static void medbayCode() throws GameActionException {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void shieldsCode() throws GameActionException {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void soldierCode() throws GameActionException{
		if (!assigned){
			joinGroup();
		}
		checkEncampment();
		int [] costs = new int[8];
		if (rc.getEnergon() < criticalHealth || checkOutnumbered()){
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
		}
		rc.yield();
	}


	private static void supplierCode() throws GameActionException{
		checkOutnumbered();
		rc.yield();
	}
	
	/**
	 * For use by all except HQ.
	 * 
	 * Check whether the number of enemies within sight range is
	 * greater than the number of friendly units.
	 * If so, message HQ.
	 * 
	 * Soldier broadcasts go to channel = soldier ID + soldierBroadcastChannelOffset
	 * Message: randomMessagingDigits and then 6 and then :
	 *		a) Digit corresponding to how significantly outnumbered a robot is (#enemies - #friends)
	 * 		b) Two digits corresponding to x coordinate of robot position (example if only 1 digit: 5 becomes 05)
	 * 		c) Two digits corresponding to y coordinate of robot position (example if only 1 digit: 5 becomes 05)
	 */
	private static boolean checkOutnumbered() throws GameActionException {
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class);
		int friendlyRobotCount = 0;
		int enemyRobotCount = 0;
		for (int i=0; i<nearbyRobots.length; i++) {
			if (nearbyRobots[i].getTeam() == rc.getTeam()) {
				friendlyRobotCount++;
			}
			else {
				enemyRobotCount++;
			}
		}
		if (friendlyRobotCount < enemyRobotCount) {
			MapLocation currentLocation = rc.getLocation();
			rc.broadcast(rc.getRobot().getID() + soldierBroadcastChannelOffset, 
					randomMessagingDigits*1000000 + 6*100000 + (enemyRobotCount - friendlyRobotCount) * 10000 +
					currentLocation.x * 100 + currentLocation.y);
			return true;
		}
		return false;
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
		int [] costs = new int [8];
		int i = 0;
		Direction bestDirection = rc.getLocation().directionTo(goal);
		Direction secondBestLeft = bestDirection.rotateLeft();
		Direction secondBestRight = bestDirection.rotateRight();
		Direction worst = bestDirection.opposite();
		Direction secondWorstLeft = worst.rotateLeft();
		Direction secondWorstRight = worst.rotateRight();
		for (Direction d:Direction.values()){
			if (!rc.canMove(d)){
				costs[i] = 1000;
			}
			else{
				MapLocation target = rc.getLocation().add(d);
				// Factor in correct direction
				if (d == bestDirection){
					costs [i] -= (2 * directionMultiplier);
				} else if (d == secondBestLeft){
					costs [i] -= (directionMultiplier);
				} else if (d == secondBestRight){
					costs [i] -= (directionMultiplier);
				} else if (d == worst){
					costs [i] += (2 * directionMultiplier);
				} else if (d == secondWorstLeft){
					costs [i] += (directionMultiplier);
				} else if (d == secondWorstRight){
					costs [i] += (directionMultiplier);
				}
				// Factor in mine defusing
				Team mineTeam = rc.senseMine(target);
				if (mineTeam == rc.getTeam().opponent() || mineTeam == Team.NEUTRAL){
					costs [i] += (mineMultiplier);
				}
			}
			i++;
		}
		return costs;
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
		int min = 999;
		int i = 0;
		for (int cost:costs){
			if (cost <= min){
				min = cost;
			}
			i++;
		}
		return Direction.values()[i];
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
		//TODO implement
	}
	
	/**
	 * For use by HQ.
	 * 
	 * Listen for soldiers and encampments reporting that they are outnumbered.
	 * Determine how to respond.
	 * Message group channels and unassigned soldiers accordingly.
	 */
	private static void manageDefenses() {
		//TODO implement
	}
	
	/**
	 * For use by HQ.
	 * 
	 * Determine priority order for upgrades and research.
	 */
	private static void researchUpgrades() {
		//TODO implement
	}
	
	/**
	 * 
	 * @return the total number of soldiers on our team that exist at a given time
	 */
	private static int countSoldiers() {
		return rc.senseNearbyGameObjects(Robot.class,(Math.max(rc.getMapHeight(),rc.getMapWidth()))^2,rc.getTeam()).length;
	}
	
	

}
