package org.hibernate;

/**
 * Thrown when a mapping is found to be invalid.
 * Similar to MappingException, but this contains more info about the path and type of mapping (e.g. file, resource or url)
 * 
 * @author Max Rydahl Andersen
 *
 */
public class InvalidMappingException extends MappingException {

	private final String path;
	private final String type;

	public InvalidMappingException(String customMessage, String type, String path, Throwable cause) {
		super(customMessage, cause);
		this.type=type;
		this.path=path;
	}
	
	public InvalidMappingException(String customMessage, String type, String path) {
		super(customMessage);
		this.type=type;
		this.path=path;
	}
	
	public InvalidMappingException(String type, String path) {
		this("Could not parse mapping document from " + type + (path==null?"":" " + path), type, path);
	}

	public InvalidMappingException(String type, String path, Throwable cause) {
		this("Could not parse mapping document from " + type + (path==null?"":" " + path), type, path, cause);		
	}

	public String getType() {
		return type;
	}
	
	public String getPath() {
		return path;
	}
}
