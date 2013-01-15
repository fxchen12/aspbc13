package KatiePlayer;

import battlecode.common.*;

import java.io.*;
import java.util.*;


public class RobotPlayer {
	
	private static RobotController rc;
	private static MapLocation rallyPoint; 
	private static MapLocation encampmentLoc;
	private static ArrayList<MapLocation> encampLocs;
	private static int timer;
	private static int currentEncampTarget;
	private static int chargeTime; //time until soldiers all rush enemy HQ

	private static int[][] neighborArray;
	private static int[] self = {2,2};
	private static int[][] surroundingIndices = new int[5][5];
	
	private static ArrayList<Integer> healingRobots;
	private static int timeCount;
	
	private static ArrayList<Integer> chargingRobots;
	private static ArrayList<Integer> quiescentRobots;
	private static ArrayList<Integer> encampmentRobots;
	
	private static int rallyTime;
	private static boolean rallying;
	
	private static double[] midline; //ax + b, [a,b]
	
		
	public static void run(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		rallyPoint = findRallyPoint();
		encampmentLoc = null;
		timer = -1;
		currentEncampTarget = 0;
		chargeTime = (Clock.getRoundNum()/400+1)*400;
		timeCount = 0;
		healingRobots = new ArrayList<Integer>();
		chargingRobots = new ArrayList<Integer>();
		quiescentRobots = new ArrayList<Integer>();
		encampmentRobots = new ArrayList<Integer>();
		rallyTime = 0;
		rallying = false;
		
		//midline code: the line that connects friendly hq to enemy hq; format: ax+b
		MapLocation us = rc.senseHQLocation();
		MapLocation them = rc.senseEnemyHQLocation();
		double a = ((double)(them.y-us.y))/((double)(them.x-us.x));
		double b = -1*a*((double)us.x)+((double)them.y);
		midline = new double[2];
		midline[0] = a;
		midline[1] = b;
		
		while (true) 
		{
			try 
			{
				timeCount++;
				if(rallying) {
					rallyTime++;
				}
				if(rc.getType() == RobotType.SOLDIER && rc.isActive()) 
				{
					soldierCode();
				}
				
				else if (rc.getType() == RobotType.HQ)
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
	
	private static void soldierCode() throws GameActionException
	{
		int ID = rc.getRobot().getID();
		if (!quiescentRobots.contains(ID) && !chargingRobots.contains(ID) && !encampmentRobots.contains(ID)) {
//			if (Math.random() < .05 || Clock.getRoundNum() < 50)
//			{
//				encampmentRobots.add(ID);
//			}
//			else {
				quiescentRobots.add(ID);
//			}
		}
		
		if (quiescentRobots.contains(ID))
		{
			Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
			
//			if (rc.getEnergon() == 40 && healingRobots.contains(ID)) {
//				healingRobots.remove(healingRobots.indexOf(ID));
//			}
//			if (healingRobots.contains(ID)) {
//				MapLocation nearestMedbay = findNearestMedbay();
//				goToLocation(nearestMedbay);
//			}
			if(enemyRobots.length==0) 
			{//no enemies nearby
				if(rc.senseEncampmentSquare(rc.getLocation()) && timeCount < 150) {
					rc.captureEncampment(RobotType.SUPPLIER);
				}
				goToLocation(rallyPoint);
			}
			else
			{//someone spotted
				
				MapLocation closestEnemy = findClosest(enemyRobots);
				smartCountNeighbors(enemyRobots,closestEnemy);
				if (isSwarm(enemyRobots.length))
				{
					avoidSwarm(closestEnemy);
				}
				else
				{
					goToLocation(closestEnemy);
				}
					
				
//				int closestDist = 1000000;
//				MapLocation closestEnemy=null;
//				for (int i=0;i<enemyRobots.length;i++)
//				{
//					Robot arobot = enemyRobots[i];
//					RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
//					int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
//					if (dist<closestDist)
//					{
//						closestDist = dist;
//						closestEnemy = arobotInfo.location;
//					}
//				}
//				

			}
		} 
		else if (encampmentRobots.contains(ID))
		{
			encampmentSoldierMove();
		}
		else
		{
			//heal the robots with HP < 10
//			if (rc.getEnergon() < 10)
//			{
//				chargingRobots.remove(chargingRobots.indexOf(ID));
//				quiescentRobots.add(ID);
//				healingRobots.add(rc.getRobot().getID());
//			}
			
			//else
			{
				goToLocation(rc.senseEnemyHQLocation());
			}
		}
		
		if (timeCount >= 100)
		{
			timeCount = 0;
			chargingRobots = quiescentRobots;
			quiescentRobots = new ArrayList<Integer>();
		}
		
//		if(rallying == true && chargingRobots.contains(rc.getRobot().getID()))
//		{
//			goToLocation(rallyPoint);
//		}
//		
//		if(rallying == true && rallyTime >=40)
//		{
//			rallying = false;
//			rallyTime = 0;
//		}
	}
	
	private static void avoidSwarm(MapLocation enemy) throws GameActionException
	{
		MapLocation us = rc.getLocation();
		double[] perpendicular = calculatePerpendicularLine(us, enemy);
		if (Math.random() < .5)
		{
			goToLocation(new MapLocation(  (us.x+10)  ,  (int)(us.y+10*perpendicular[0])));
		}
		else
		{
			goToLocation(new MapLocation(us.x-10,(int)(us.y-10*perpendicular[0])));
		}
	}
	
	private static double[] calculatePerpendicularLine(MapLocation us, MapLocation them)
	{
		double[] arr = new double[2];
		if (them.x-us.x== 0 )
		{
			System.out.println("Line error");
		}
		double a = ((double)(them.y-us.y))/((double)(them.x-us.x));
		double a2 = -1.0/a;
		double b = (double)us.y - (a2* (double)us.x);
		arr[0] = a2;
		arr[1] = b;
		return arr;
	}
	
	private static int distance(MapLocation a, MapLocation b)
	{
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}
	
	private static MapLocation findNearestMedbay() throws GameActionException
	{
		MapLocation[] allied = rc.senseAlliedEncampmentSquares();
		MapLocation curr = rc.getLocation();
		int minDist = 1000000;
		MapLocation nearestMedbay = encampLocs.get(0);
		
		for(int i=0; i<allied.length; i++)
		{
			GameObject x = rc.senseObjectAtLocation(allied[i]);
			if (x instanceof Robot)
			{
				if(rc.senseRobotInfo((Robot)x).type == RobotType.MEDBAY)
				{
					int d = distance(curr,allied[i]);
					if (d<minDist) {
						minDist = d;
						nearestMedbay = allied[i];
					}
				}
			}
		}
		return nearestMedbay;
	}
	
	private static void encampmentSoldierMove() throws GameActionException{
		//see if an encampment is nearby
		if(rc.senseEncampmentSquare(rc.getLocation())) {
//			if (currentEncampTarget %8 == 0)
//			{
//				rc.captureEncampment(RobotType.MEDBAY);
//			}
//			else
			{
				
				rc.captureEncampment(RobotType.SUPPLIER);
			}
		}
		else {
			if (encampLocs == null)
			{
				MapLocation[] locs = rc.senseEncampmentSquares(rc.senseHQLocation(),100000,Team.NEUTRAL);
				locs = mergeSort(locs, 0, locs.length);
				encampLocs = new ArrayList<MapLocation>(Arrays.asList(locs));
			}
			//if so, set encampmentLoc to that encampment
			if (encampLocs.size() > currentEncampTarget)
			{
				//timer keeps track of whether encampment is captured or not
//				if (timer == -1)
//				{
//					timer = Clock.getRoundNum() + 55;
//					currentEncampTarget++;
//					if (currentEncampTarget > encampLocs.size()) {
//						currentEncampTarget = 0;
//					}
//				}

				MapLocation destination = encampLocs.get(currentEncampTarget);
				goToLocation(destination);
				//timer--;
			}
			else
			{
				goToLocation(rallyPoint);
			}
		}
	}
	
	private static MapLocation findClosest(MapLocation a, MapLocation[]locs) {
		int closestDist = 1000000;
		MapLocation closestLoc = null;
		for (int i=0; i<locs.length; i++)
		{
			int dist = distance(a,locs[i]);
			if (dist < closestDist)
			{
				closestDist = dist;
				closestLoc = locs[i];
			}
		}
		return closestLoc;
	}
	
	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int weight = Clock.getRoundNum() / 50 + 1;
		int x = (enemyLoc.x+3*ourLoc.x)/(4);
		int y = (enemyLoc.y+3*ourLoc.y)/(4);
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}
	
	private static void goToLocation(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		int nearbyRobotCount = 0;
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
				else
				{
					nearbyRobotCount ++;
					MapLocation curr = rc.getLocation();
					if (Math.abs(curr.x - whereToGo.x) + Math.abs(curr.y - whereToGo.y) < 3) {
						currentEncampTarget++;
					}
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
			if (rc.canMove(dir) && rc.getTeamPower() > 100)
				rc.spawn(dir);
		}
	}
	
	
//SORTING
	public static MapLocation[] mergeSort(MapLocation[] arr, int begin, int end)
	{
		if (end-begin == 1)
		{
			return arr;
		}
		int mid = (begin+end)/2;
		mergeSort(arr,begin,mid);
		mergeSort(arr,mid,end);
		merge(arr,begin,mid,end);
		return arr;
	}
	
	public static MapLocation[] merge(MapLocation[]arr, int begin, int mid, int end)
	{
		MapLocation[] temp = new MapLocation[end-begin];
		int arrindex = 0;
		int i = begin;
		int j = mid;
		
		MapLocation HQLoc = rc.senseHQLocation();
		
		while (i<mid || j< end)
		{
			if (j == end || (i<mid && j<end && 
					distanceToLine(arr[i]) < distanceToLine(arr[j])))
			{
				temp[arrindex] = arr[i];
				i++;
			}
			else
			{
				temp[arrindex] = arr[j];
				j++;
			}
			
			arrindex ++;
		}
		
		for(int k=0; k< temp.length; k++)
		{
			arr[begin + k] = temp[k];
		}
		
		return arr;
	}
	
	private static double distanceToLine(MapLocation loc)
	{
		double y = midline[0]*(double)loc.x + midline[1];
		return Math.abs(y - (double)loc.y);
	}
	
	
	private static MapLocation findClosest(Robot[] enemyRobots) throws GameActionException {
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
		return closestEnemy;
	}

//	ARRAY-BASED NEIGHBOR DETECTION
	private static void smartCountNeighbors(Robot[] enemyRobots,MapLocation closestEnemy) throws GameActionException{
		//build a 5 by 5 array of neighboring units
		neighborArray = populateNeighbors(new int[5][5]);/*1500*/
		//get the total number of enemies and allies adjacent to each of the 8 adjacent tiles
		int[] adj = totalAllAdjacent(neighborArray);/*2500*/
		
		//also check your current position
		int me = totalAdjacent(neighborArray,self);
		
		//display the neighbor information to the indicator strings
		//rc.setIndicatorString(0, "adjacent: "+intListToString(adj)+" me: "+me);
		//note: if the indicator string says 23, that means 2 enemies and 3 allies.
		
		//TODO: Now act on that data. I leave this to you. 
		
	}
	public static int[] locToIndex(MapLocation ref, MapLocation test,int offset){/*40*/
		int[] index = new int[2];
		index[0] = test.y-ref.y+offset;
		index[1] = test.x-ref.x+offset;
		return index;
	}
	public static int[][] initSurroundingIndices(Direction forward){
		int[][] indices = new int[8][2];
//		Direction forward =rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		int startOrdinal = forward.ordinal();
		MapLocation myLoc = rc.getLocation();
		for(int i=0;i<8;i++){
			indices[i] = locToIndex(myLoc,myLoc.add(Direction.values()[(i+startOrdinal)%8]),0);
		}
		return indices;
	}
	public static String arrayToString(int[][] array){
		String outstr = "";
		for(int i=0;i<5;i++){
			outstr = outstr + "; ";
			for(int j=0;j<5;j++)
				outstr = outstr+array[i][j]+" ";
		}
		return outstr;
	}
	public static int[][] populateNeighbors(int[][] array) throws GameActionException{/*788*/
		MapLocation myLoc=rc.getLocation();
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,8);
//		rc.setIndicatorString(2, "number of bots: "+nearbyRobots.length);
		for (Robot aRobot:nearbyRobots){
			RobotInfo info = rc.senseRobotInfo(aRobot);
			int[] index = locToIndex(myLoc,info.location,2);
			if(index[0]>=0&&index[0]<=4&&index[1]>=0&&index[1]<=4){
				if(info.team==rc.getTeam()){
					array[index[0]][index[1]]=1;//1 is allied
				}else{
					array[index[0]][index[1]]=10;//10 is enemy
				}
			}
		}
		return array;
	}
	public static int totalAdjacent(int[][] neighbors,int[] index){/*270*/
		int total = 0;
		for(int i=0;i<8;i++){
			total = total+neighbors[index[0]+surroundingIndices[i][0]][index[1]+surroundingIndices[i][1]];
		}
		return total;
	}
	public static int[] addPoints(int[] p1, int[] p2){/*30*/
		int[] tot = new int[2];
		tot[0] = p1[0]+p2[0];
		tot[1] = p1[1]+p2[1];
		return tot;
	}
	public static int[] totalAllAdjacent(int[][] neighbors){/*2454*/
		//TODO compute only on open spaces (for planned movement)
		int[] allAdjacent = new int[8];
		for(int i=0;i<8;i++){
			allAdjacent[i] =  totalAdjacent(neighbors,addPoints(self,surroundingIndices[i]));
		}
		return allAdjacent;
	}
//heuristic: goodness or badness of a neighbor int, which includes allies and enemies
	
	public static double howGood(int neighborInt){
		double goodness = 0;
		double numberOfAllies = neighborInt%10;
		double numberOfEnemies = neighborInt-numberOfAllies;
		goodness = numberOfAllies-numberOfEnemies;
		return goodness;
	}
	public static boolean isSwarm(int numberOfEnemies){
		if (numberOfEnemies>=5){
			return true;
		}
		else{
			return false;
		}
		
	}
}

	
