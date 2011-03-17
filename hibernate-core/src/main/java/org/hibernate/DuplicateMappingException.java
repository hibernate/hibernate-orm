/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
 * Raised whenever a duplicate for a certain type occurs.
 * Duplicate class, table, property name etc.
 * 
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class DuplicateMappingException extends MappingException {
	public static enum Type {
		ENTITY,
		TABLE,
		PROPERTY,
		COLUMN
	}

	private final String name;
	private final String type;

	public DuplicateMappingException(Type type, String name) {
		this( type.name(), name );
	}

	@Deprecated
	public DuplicateMappingException(String type, String name) {
		this( "Duplicate " + type + " mapping " + name, type, name );
	}

	public DuplicateMappingException(String customMessage, Type type, String name) {
		this( customMessage, type.name(), name );
	}

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
