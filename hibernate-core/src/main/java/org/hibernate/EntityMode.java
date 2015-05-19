/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.Locale;

/**
 * Defines the representation modes available for entities.
 *
 * @author Steve Ebersole
 */
public enum EntityMode {
	/**
	 * The {@code pojo} entity mode describes an entity model made up of entity classes (loosely) following
	 * the java bean convention.
	 */
	POJO( "pojo" ),

	/**
	 * The {@code dynamic-map} entity mode describes an entity model defined using {@link java.util.Map} references.
	 */
	MAP( "dynamic-map" );

	private final String externalName;

	private EntityMode(String externalName) {
		this.externalName = externalName;
	}

	public String getExternalName() {
		return externalName;
	}

	@Override
	public String toString() {
		return externalName;
	}

	/**
	 * Legacy-style entity-mode name parsing.  <b>Case insensitive</b>
	 *
	 * @param entityMode The entity mode name to evaluate
	 *
	 * @return The appropriate entity mode; {@code null} for incoming {@code entityMode} param is treated by returning
	 * {@link #POJO}.
	 */
	public static EntityMode parse(String entityMode) {
		if ( entityMode == null ) {
			return POJO;
		}
		if ( MAP.externalName.equalsIgnoreCase( entityMode ) ) {
			return MAP;
		}
		return valueOf( entityMode.toUpperCase( Locale.ENGLISH ) );
	}

}
