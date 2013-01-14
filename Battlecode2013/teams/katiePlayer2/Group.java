package katiePlayer2;

import battlecode.common.*;

public class Group {
	private int size;
	private int mission;
	private MapLocation goal;
	private int channel;
	
	public Group(int size, int mission, MapLocation goal, int channel) {
		super();
		this.size = size;
		this.mission = mission;
		this.goal = goal;
		this.channel = channel;
	}
	
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getMission() {
		return mission;
	}
	public void setMission(int mission) {
		this.mission = mission;
	}
	public MapLocation getGoal() {
		return goal;
	}
	public void setGoal(MapLocation goal) {
		this.goal = goal;
	}
	public int getChannel() {
		return channel;
	}
	public void setChannel(int channel) {
		this.channel = channel;
	}
	
}
