package org.hibernate.tool.hbmlint;

public class Issue {

	public static final int HIGH_PRIORITY = 100;
	public static final int NORMAL_PRIORITY = 50;
	public static final int LOW_PRIORITY = 0;

	private final String type;
	private final int priority;
	
	private final String description;

	public Issue(String type, int priority, String description) {
		this.description = description;
		this.priority = priority;
		this.type = type;
	}
	
	public String toString() {
		return type + ":" + description;
	}

	public String getDescription() {
		return description;
	}
	
	public int getPriority() {
		return priority;
	}
}
