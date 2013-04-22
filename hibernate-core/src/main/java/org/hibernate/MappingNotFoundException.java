/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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

/**
 * Thrown when a resource for a mapping could not be found.
 * 
 * @author Max Rydahl Andersen
 */
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
