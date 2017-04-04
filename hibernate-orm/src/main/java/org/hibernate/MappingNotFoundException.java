/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when a resource for a mapping could not be found.
 * 
 * @author Max Rydahl Andersen
 *
 * @deprecated Use {@link org.hibernate.boot.MappingNotFoundException} instead.
 */
@Deprecated
public class MappingNotFoundException extends MappingException {
	private final String path;
	private final String type;

	/**
	 * Constructs a MappingNotFoundException using the given information.
	 *
	 * @param customMessage A message explaining the exception condition
	 * @param type The type of mapping that could not be found
	 * @param path The path (type specific) of the mapping that could not be found
	 * @param cause The underlying cause
	 */
	public MappingNotFoundException(String customMessage, String type, String path, Throwable cause) {
		super( customMessage, cause );
		this.type = type;
		this.path = path;
	}

	/**
	 * Constructs a MappingNotFoundException using the given information.
	 *
	 * @param customMessage A message explaining the exception condition
	 * @param type The type of mapping that could not be found
	 * @param path The path (type specific) of the mapping that could not be found
	 */
	public MappingNotFoundException(String customMessage, String type, String path) {
		super( customMessage );
		this.type = type;
		this.path = path;
	}

	/**
	 * Constructs a MappingNotFoundException using the given information, using a standard message.
	 *
	 * @param type The type of mapping that could not be found
	 * @param path The path (type specific) of the mapping that could not be found
	 */
	public MappingNotFoundException(String type, String path) {
		this( type + ": " + path + " not found", type, path );
	}

	/**
	 * Constructs a MappingNotFoundException using the given information, using a standard message.
	 *
	 * @param type The type of mapping that could not be found
	 * @param path The path (type specific) of the mapping that could not be found
	 * @param cause The underlying cause
	 */
	public MappingNotFoundException(String type, String path, Throwable cause) {
		this( type + ": " + path + " not found", type, path, cause );
	}

	public String getType() {
		return type;
	}
	
	public String getPath() {
		return path;
	}
}
