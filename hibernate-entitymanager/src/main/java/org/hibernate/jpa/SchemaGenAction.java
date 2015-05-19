/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public static SchemaGenAction interpret(Object value) {
		if ( value == null ) {
			return NONE;
		}

		if ( SchemaGenAction.class.isInstance( value ) ) {
			return (SchemaGenAction) value;
		}

		final String name = value.toString();
		if ( StringHelper.isEmpty( name ) || NONE.externalName.equals( name ) ) {
			// default is NONE
			return NONE;
		}

		if ( CREATE.externalName.equals( name ) ) {
			return CREATE;
		}
		else if ( DROP.externalName.equals( name ) ) {
			return DROP;
		}
		else if ( BOTH.externalName.equals( name ) ) {
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
