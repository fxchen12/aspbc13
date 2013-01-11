package francisPlayer;

import java.util.Arrays;

import battlecode.common.*;

public class RobotPlayer {
	
	public static RobotController rc;
	private static MapLocation goal;
	private static int mission = 4;
	private static boolean isAssigned = false; // Whether or not the robot has actually been given a mission
	private static int soldierNumber = 30; // TODO Calculate a number that is an integer, based on known bytecode costs of soldiers, map size, etc.
	private static MapLocation[] encampments;
	
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
	public static void artilleryCode(){
		
	}
	public static void generatorCode(){
		
	}
	private static void hqCode(){
		// Populate list of encampments to be captured
		if(Clock.getRoundNum() == 0){
			encampments = rc.senseAllEncampmentSquares();
		}
		// Spawn troops if empty space around the base exists
		// Spawn troops if there is power to spare
		if (countSoldiers() < soldierNumber){
			for (Direction d:Direction.values()){
				if (rc.canMove(d)){
					try {
						rc.spawn(d);
						broadcastMission();
					} catch (GameActionException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		// If there is no power to spare for troops, research upgrades
		}
		else{
			try{
				rc.researchUpgrade(Upgrade.DEFUSION);
				rc.researchUpgrade(Upgrade.FUSION);
				rc.researchUpgrade(Upgrade.VISION);
				rc.researchUpgrade(Upgrade.PICKAXE);
				rc.researchUpgrade(Upgrade.NUKE);
			}
			catch(GameActionException e){
				e.printStackTrace();
			}
		}
		// End turn
		rc.yield();
	}


	
	private static void medbayCode() {
	
	}
	private static void shieldsCode() {
	
	}
	private static void soldierCode() {
		goal = rc.senseHQLocation(); // by default, robots rally at allied HQ
		if (!isAssigned){
			determineMission();
		}
		
		// TODO Navigation code goes here
		// TODO Greedy algorithm
		// TODO Mines incur a cost
		// TODO Direction to the goal is the least cost
	}
	
	private static void supplierCode() {
	
	}
	
	private static void broadcastMission() {
		// TODO work on hacking by repeating messages not sent by our team, slightly scrambled
		// TODO (approach inspired by Kevin Yue's account of a previous Battlecode competition)
		// TODO work on hacking by using spare bytecode to send random messages
		// TODO work on encoding messages with a unique code at the beginning
		// TODO 1 = capture, 2 = defend, 3 = attack, 4 = rally
		// TODO Map coordinates can be sent using the other digits
		// TODO Example: 12345 = capture encampment at 23,45
	}
	
	private static void determineMission() {
		
	}
	
	
	private static int countSoldiers(){
		return rc.senseNearbyGameObjects(Robot.class,Math.max(rc.getMapHeight(),rc.getMapWidth()),rc.getTeam()).length;
	}
}
