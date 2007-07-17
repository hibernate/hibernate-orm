package org.hibernate;

/**
 * Thrown when a resource for a mapping could not be found.
 * 
 * @author Max Rydahl Andersen
 *
 */
public class MappingNotFoundException extends MappingException {

	private final String path;
	private final String type;

	public MappingNotFoundException(String customMessage, String type, String path, Throwable cause) {
		super(customMessage, cause);
		this.type=type;
		this.path=path;
	}
	
	public MappingNotFoundException(String customMessage, String type, String path) {
		super(customMessage);
		this.type=type;
		this.path=path;
	}
	
	public MappingNotFoundException(String type, String path) {
		this(type + ": " + path + " not found", type, path);
	}

	public MappingNotFoundException(String type, String path, Throwable cause) {
		this(type + ": " + path + " not found", type, path, cause);
	}

	public String getType() {
		return type;
	}
	
	public String getPath() {
		return path;
	}
}
