/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.annotations;

/**
 * Where should Hibernate retrieve the value from? From the database, or from the current JVM?
 *
 * @author Hardy Ferentschik
 */
public enum SourceType {
	/**
	 * Get the timestamp from the current VM.
	 */
	VM( "timestamp" ),

	/**
	 * Get the timestamp from the database.
	 */
	DB( "dbtimestamp" );

	private final String typeName;

	private SourceType(String typeName ) {
		this.typeName = typeName;
	}

	/**
	 * Get the corresponding Hibernate {@link org.hibernate.type.VersionType} name.
	 *
	 * @return The corresponding type name.
	 */
	public String typeName() {
		return typeName;
	}
}
