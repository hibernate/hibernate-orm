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
 * Describes the allowable values of the {@value AvailableSettings#SCHEMA_GEN_DATABASE_ACTION} and
 * {@link AvailableSettings#SCHEMA_GEN_SCRIPTS_ACTION} settings.
 *
 * @see AvailableSettings#SCHEMA_GEN_DATABASE_ACTION
 * @see AvailableSettings#SCHEMA_GEN_SCRIPTS_ACTION
 *
 * @author Steve Ebersole
 */
public enum SchemaGenAction {
	/**
	 * "none" - no actions will be performed (aka, generation is disabled).
	 */
	NONE( "none" ),
	/**
	 * "create" - database creation will be generated
	 */
	CREATE( "create" ),
	/**
	 * "drop" - database dropping will be generated
	 */
	DROP( "drop" ),
	/**
	 * "drop-and-create" - both database creation and database dropping will be generated.
	 */
	BOTH( "drop-and-create" );

	private final String externalName;

	private SchemaGenAction(String externalName) {
		this.externalName = externalName;
	}

	/**
	 * Used when processing JPA configuration to interpret the user config values.
	 *
	 * @param value The encountered config value
	 *
	 * @return The matching enum value.  An empty value will return {@link #NONE}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static SchemaGenAction interpret(String value) {
		if ( StringHelper.isEmpty( value ) || NONE.externalName.equals( value ) ) {
			// default is NONE
			return NONE;
		}

		if ( CREATE.externalName.equals( value ) ) {
			return CREATE;
		}
		else if ( DROP.externalName.equals( value ) ) {
			return DROP;
		}
		else if ( BOTH.externalName.equals( value ) ) {
			return BOTH;
		}

		throw new IllegalArgumentException(
				String.format(
						"Unrecognized '%s' or '%s' value : %s",
						AvailableSettings.SCHEMA_GEN_DATABASE_ACTION,
						AvailableSettings.SCHEMA_GEN_SCRIPTS_ACTION,
						value
				)
		);
	}

	public static SchemaGenAction interpret(Object value) {
		if ( SchemaGenAction.class.isInstance( value ) ) {
			return (SchemaGenAction) value;
		}

		return interpret( value == null ? null : value.toString() );
	}

	/**
	 * Does this action include creations?
	 *
	 * @return {@code true} if this action is either {@link #CREATE} or {@link #BOTH}
	 */
	public boolean includesCreate() {
		return this == CREATE || this == BOTH;
	}

	/**
	 * Does this action include drops?
	 *
	 * @return {@code true} if this action is either {@link #DROP} or {@link #BOTH}
	 */
	public boolean includesDrop() {
		return this == DROP || this == BOTH;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + externalName + ")";
	}
}
