/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.internal.util.xml.XmlDocument;

/**
 * Thrown when a mapping is found to be invalid.
 *
 * Similar to MappingException, but this contains more info about the path and type of
 * mapping (e.g. file, resource or url)
 * 
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class InvalidMappingException extends MappingException {
	private final String path;
	private final String type;

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param type The type of invalid mapping document
	 * @param path The path (type specific) of the invalid mapping document
	 * @param cause The underlying cause
	 */
	public InvalidMappingException(String customMessage, String type, String path, Throwable cause) {
		super( customMessage, cause );
		this.type = type;
		this.path = path;
	}

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param type The type of invalid mapping document
	 * @param path The path (type specific) of the invalid mapping document
	 */
	public InvalidMappingException(String customMessage, String type, String path) {
		super( customMessage );
		this.type=type;
		this.path=path;
	}

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param xmlDocument The document that was invalid
	 * @param cause The underlying cause
	 */
	public InvalidMappingException(String customMessage, XmlDocument xmlDocument, Throwable cause) {
		this( customMessage, xmlDocument.getOrigin().getType(), xmlDocument.getOrigin().getName(), cause );
	}

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param xmlDocument The document that was invalid
	 */
	public InvalidMappingException(String customMessage, XmlDocument xmlDocument) {
		this( customMessage, xmlDocument.getOrigin().getType(), xmlDocument.getOrigin().getName() );
	}

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param origin The origin of the invalid mapping document
	 */
	public InvalidMappingException(String customMessage, Origin origin) {
		this( customMessage, origin.getType().getLegacyTypeText(), origin.getName() );
	}

	/**
	 * Constructs an InvalidMappingException using the given information and a standard message.
	 *
	 * @param type The type of invalid mapping document
	 * @param path The path (type specific) of the invalid mapping document
	 */
	public InvalidMappingException(String type, String path) {
		this( "Could not parse mapping document from " + type + (path==null?"":" " + path), type, path );
	}

	/**
	 * Constructs an InvalidMappingException using the given information and a standard message.
	 *
	 * @param type The type of invalid mapping document
	 * @param path The path (type specific) of the invalid mapping document
	 * @param cause The underlying cause
	 */
	public InvalidMappingException(String type, String path, Throwable cause) {
		this( "Could not parse mapping document from " + type + (path==null?"":" " + path), type, path, cause );
	}

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param origin The origin of the invalid mapping document
	 * @param cause The underlying cause
	 */
	public InvalidMappingException(String customMessage, org.hibernate.internal.util.xml.Origin origin, Exception cause) {
		this( customMessage, origin.getType(), origin.getName(), cause );
	}

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param origin The origin of the invalid mapping document
	 */
	public InvalidMappingException(String customMessage, org.hibernate.internal.util.xml.Origin origin) {
		this( customMessage, origin, null );
	}

	public String getType() {
		return type;
	}
	
	public String getPath() {
		return path;
	}
}
