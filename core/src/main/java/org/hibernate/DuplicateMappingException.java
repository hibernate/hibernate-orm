package org.hibernate;

/**
 * Raised whenever a duplicate for a certain type occurs.
 * Duplicate class, table, property name etc.
 * 
 * @author Max Rydahl Andersen
 *
 */
public class DuplicateMappingException extends MappingException {

	private final String name;
	private final String type;

	public DuplicateMappingException(String customMessage, String type, String name) {
		super(customMessage);
		this.type=type;
		this.name=name;
	}
	
	public DuplicateMappingException(String type, String name) {
		this("Duplicate " + type + " mapping " + name, type, name);
	}

	public String getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
}
