package francisPlayer2;

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
	private static ArrayList<Group> groupsList;
	private static ArrayList<Integer> newSoldiers;
	// Constants
	private static int criticalRangeSquared = 32; // TODO optimize
	private static int criticalHealth = 10; // TODO optimize
	private static int rallyRange = 10; // TODO optimize
	
	public static void run (RobotController myRC){
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

	private static void artilleryCode() {
		checkOutnumbered();
		rc.yield();
	}

	private static void generatorCode() {
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
	

	private static void medbayCode() {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void shieldsCode() {
		checkOutnumbered();
		rc.yield();
	}
	
	private static void soldierCode() {
		if (!assigned){
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
		}
		rc.yield();
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
