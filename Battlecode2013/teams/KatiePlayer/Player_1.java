package KatiePlayer;

import battlecode.common.*;


public class Player_1 {
	
	private static RobotController rc;
	private static MapLocation rallyPoint; 
	private static boolean isEncampmentSoldier;
	private static MapLocation encampmentLoc;
	
	public static void run(RobotController myRC) 
	{
		rc = myRC;
		rallyPoint = findRallyPoint();
		if (Math.random() < .5) {
			isEncampmentSoldier = true;
			MapLocation[] encampmentLocs = rc.senseEncampmentSquares(rc.senseHQLocation(),100000,Team.NEUTRAL);
			if (encampmentLocs.length < Clock.getRoundNum()%50 + 1)
			{
				encampmentLoc = encampmentLocs[Clock.getRoundNum()%50];
			}
			else
			{
				encampmentLoc = null;
				isEncampmentSoldier = false;
			}
		}
		else {
			isEncampmentSoldier = false;
			encampmentLoc = null;
		}
		
		while (true) 
		{
			try 
			{
				if(rc.getType() == RobotType.SOLDIER && rc.isActive()) 
				{
					if (Clock.getRoundNum()<200)
					{
						Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
						if(enemyRobots.length==0) 
						{//no enemies nearby
							if(Math.random() < .01) //randomly drop mines
							{
								if(rc.senseMine(rc.getLocation())==null)
									rc.layMine();
							}
							if(isEncampmentSoldier)
							{
								//go to correct encampment
								goToLocation(encampmentLoc);
							}
							else
							{
								goToLocation(rallyPoint);
							}
						}
						else
						{//someone spotted
							int closestDist = 1000000;
							MapLocation closestEnemy=null;
							for (int i=0;i<enemyRobots.length;i++)
							{
								Robot arobot = enemyRobots[i];
								RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
								int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
								if (dist<closestDist)
								{
									closestDist = dist;
									closestEnemy = arobotInfo.location;
								}
							}
							goToLocation(closestEnemy);
						}
					} 
					else //blitz the enemy
					{ 
						goToLocation(rc.senseEnemyHQLocation());
					}
				}
				else
				{
					HQSpawn();
				}
				// End turn
				rc.yield();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}
	
	private static void goToLocation(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			int[] directionOffsets = {0,1,-1,2,-2};
			Direction lookingAtCurrently = null;
			lookAround: for (int d:directionOffsets){
				lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(lookingAtCurrently)){
					moveOrDefuse(lookingAtCurrently);
					break lookAround;
				}
			}
		}
	}
	private static void moveOrDefuse(Direction dir) throws GameActionException{
		MapLocation ahead = rc.getLocation().add(dir);
		if(rc.senseMine(ahead)!= null){
			rc.defuseMine(ahead);
		}else{
			rc.move(dir);			
		}
	}
	
	public static void HQSpawn() throws GameActionException{
		if (rc.isActive()) {
			// Spawn a soldier
			Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.canMove(dir))
				rc.spawn(dir);
		}
	}
}
