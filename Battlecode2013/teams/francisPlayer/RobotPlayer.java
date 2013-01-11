package francisPlayer;

import battlecode.common.*;

public class RobotPlayer {
	
	public static RobotController rc;
	
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
	private static void hqCode() {
		// TODO Auto-generated method stub
		
	}

	private static void medbayCode() {
	// TODO Auto-generated method stub
	
	}
	private static void shieldsCode() {
	// TODO Auto-generated method stub
	
	}
	private static void soldierCode() {
	// TODO Auto-generated method stub
	
	}
	private static void supplierCode() {
	// TODO Auto-generated method stub
	
	}
}
