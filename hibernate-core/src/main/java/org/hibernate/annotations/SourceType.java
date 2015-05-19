/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
