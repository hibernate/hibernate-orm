/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa;

import org.hibernate.internal.util.StringHelper;

/**
 * Describes the allowable values of the {@value AvailableSettings#SCHEMA_GEN_TARGET} setting.
 *
 * @see AvailableSettings#SCHEMA_GEN_TARGET
 *
 * @author Steve Ebersole
 */
public enum SchemaGenTarget {
	/**
	 * "database" - Generation commands will be executed directly against the database (via JDBC Statements).
	 */
	DATABASE( "database" ),
	/**
	 * "scripts" - Generation commands will be written to script (text) "targets" as indicated by the
	 * {@value AvailableSettings#SCHEMA_GEN_CREATE_SCRIPT_TARGET} and
	 * {@value AvailableSettings#SCHEMA_GEN_DROP_SCRIPT_TARGET} settings.
	 */
	SCRIPTS( "scripts" ),
	/**
	 * "database-and-scripts" - Generation commands will be sent to both.
	 *
	 * @see #DATABASE
	 * @see #SCRIPTS
	 */
	BOTH( "database-and-scripts" );

	private final String externalName;

	private SchemaGenTarget(String externalName) {
		this.externalName = externalName;
	}

	/**
	 * Used when processing JPA configuration to interpret the {@value AvailableSettings#SCHEMA_GEN_TARGET} setting.
	 *
	 * @param value The encountered value of {@value AvailableSettings#SCHEMA_GEN_TARGET}
	 *
	 * @return The matching enum value.  An empty value will return {@code null}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static SchemaGenTarget interpret(String value) {
		if ( StringHelper.isEmpty( value ) ) {
			// empty is in fact valid as means to interpret default value based on other settings
			return null;
		}

		if ( DATABASE.externalName.equals( value ) ) {
			return DATABASE;
		}
		else if ( SCRIPTS.externalName.equals( value ) ) {
			return SCRIPTS;
		}
		else if ( BOTH.externalName.equals( value ) ) {
			return BOTH;
		}

		throw new IllegalArgumentException( "Unknown schema generation target value : " + value );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + externalName + ")";
	}
}
