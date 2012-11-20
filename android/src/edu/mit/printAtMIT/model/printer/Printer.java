package edu.mit.printAtMIT.model.printer;

public class Printer {
	public static final int AVAILABLE_STATUS = 0;
	public static final int BUSY_STATUS = 1;
	public static final int ERROR_STATUS = 2;
	public static final int UNKNOWN_STATUS = 3;
	private String name;
	private String sectionHeader;
	private String location;
	private String building;
	private boolean atResidence;
	private int status;
	private double distance = 0.0;
	private int latitude;
	private int longitude;
	
	public Printer(String name, String sectionHeader, String location,
			String building, boolean atResidence, int status, int latitude, int longitude, double distance) {
		this.name = name;
		this.sectionHeader = sectionHeader;
		this.location = location;
		this.building = building;
		this.atResidence = atResidence;
		this.latitude = latitude;
		this.longitude = longitude;
		this.status = UNKNOWN_STATUS;
		if (status == AVAILABLE_STATUS) {
			this.status = AVAILABLE_STATUS;
		}
		else if (status == BUSY_STATUS) {
			this.status = BUSY_STATUS;
		}
		else if (status == ERROR_STATUS) {
			this.status = ERROR_STATUS;
		}
		this.distance = distance;
	}
	
	//getters
	public String getName() {
		return new String(this.name);
	}
	
	public String getSectionHeader() {
		return new String(this.sectionHeader);
	}
	
	public String getLocation() {
		return new String(this.location);
	}
	
	public String getBuilding() {
		return new String(this.building);
	}
	
	public boolean atResidence() {
		return this.atResidence;
	}
	public int getStatus() {
		return this.status;
	}
	
	public double getDistance() {
		return this.distance;
	}
	
	public int getLatitude() {
	    return this.latitude;
	}
	
	public int getLongitude() {
	    return this.longitude;
	}
}
