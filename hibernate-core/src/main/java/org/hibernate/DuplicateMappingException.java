/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2011, Red Hat Inc. or third-party contributors as
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
 * Raised whenever a duplicate for a certain type occurs.  Duplicate class, table, property name etc.
 * 
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class DuplicateMappingException extends MappingException {
	/**
	 * Enumeration of the types of things that can be duplicated.
	 */
	public static enum Type {
		/**
		 * A duplicate entity definition was encountered.
		 */
		ENTITY,
		/**
		 * A duplicate table definition was encountered.
		 */
		TABLE,
		/**
		 * A duplicate property/attribute definition was encountered.
		 */
		PROPERTY,
		/**
		 * A duplicate column definition was encountered.
		 */
		COLUMN
	}

	private final String name;
	private final String type;

	/**
	 * Creates a DuplicateMappingException using the given type and name.
	 *
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 */
	public DuplicateMappingException(Type type, String name) {
		this( type.name(), name );
	}

	/**
	 * Creates a DuplicateMappingException using the given type and name.
	 *
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 *
	 * @deprecated Use the for taking {@link Type} instead.
	 */
	@Deprecated
	public DuplicateMappingException(String type, String name) {
		this( "Duplicate " + type + " mapping " + name, type, name );
	}

	/**
	 * Creates a DuplicateMappingException using the given customMessage, type and name.
	 *
	 * @param customMessage A custom exception message explaining the exception condition
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 */
	public DuplicateMappingException(String customMessage, Type type, String name) {
		this( customMessage, type.name(), name );
	}

	/**
	 * Creates a DuplicateMappingException using the given customMessage, type and name.
	 *
	 * @param customMessage A custom exception message explaining the exception condition
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 *
	 * @deprecated Use the for taking {@link Type} instead.
	 */
	@Deprecated
	public DuplicateMappingException(String customMessage, String type, String name) {
		super( customMessage );
		this.type=type;
		this.name=name;
	}

	public String getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
}
