/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 20082011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;

import org.hibernate.internal.jaxb.Origin;
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
		this( customMessage, origin.getType().toString(), origin.getName() );
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
