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
 * Similar to MappingException, but this contains more info about the path and type of mapping (e.g. file, resource or url)
 * 
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
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

	public InvalidMappingException(String customMessage, XmlDocument xmlDocument, Throwable cause) {
		this( customMessage, xmlDocument.getOrigin().getType(), xmlDocument.getOrigin().getName(), cause );
	}

	public InvalidMappingException(String customMessage, XmlDocument xmlDocument) {
		this( customMessage, xmlDocument.getOrigin().getType(), xmlDocument.getOrigin().getName() );
	}

	public InvalidMappingException(String customMessage, Origin origin) {
		this( customMessage, origin.getType().toString(), origin.getName() );
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
