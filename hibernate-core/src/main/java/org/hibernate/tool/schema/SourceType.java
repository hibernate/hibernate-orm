/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.StringHelper;

/**
 * Enumeration of the various types of sources understood by various SchemaManagementTooling
 * delegates.
 *
 * @see AvailableSettings#HBM2DDL_CREATE_SOURCE
 * @see AvailableSettings#HBM2DDL_DROP_SOURCE
 *
 * @author Steve Ebersole
 */
public enum SourceType {
	/**
	 * "metadata" - The O/RM metadata is used as the exclusive source for generation
	 */
	METADATA( "metadata" ),
	/**
	 * "script" - External DDL script(s) are used as the exclusive source for generation.  The scripts for schema
	 * creation and dropping come from different sources.  The creation DDL script is identified by the
	 * {@value AvailableSettings#HBM2DDL_CREATE_SCRIPT_SOURCE} setting; the drop DDL script is identified by the
	 * {@value AvailableSettings#HBM2DDL_DROP_SCRIPT_SOURCE} setting.
	 *
	 * @see AvailableSettings#HBM2DDL_CREATE_SCRIPT_SOURCE
	 * @see AvailableSettings#HBM2DDL_DROP_SCRIPT_SOURCE
	 */
	SCRIPT( "script" ),
	/**
	 * "metadata-then-scripts" - Both the O/RM metadata and external DDL scripts are used as sources for generation,
	 * with the O/RM metadata being applied first.
	 *
	 * @see #METADATA
	 * @see #SCRIPT
	 */
	METADATA_THEN_SCRIPT( "metadata-then-script" ),
	/**
	 * "scripts-then-metadata" - Both the O/RM metadata and external DDL scripts are used as sources for generation,
	 * with the commands from the external DDL script(s) being applied first
	 *
	 * @see #SCRIPT
	 * @see #METADATA
	 */
	SCRIPT_THEN_METADATA( "script-then-metadata" );

	private final String externalName;

	SourceType(String externalName) {
		this.externalName = externalName;
	}

	/**
	 * Used when processing JPA configuration to interpret the user config value
	 *
	 * @param value The encountered user config value
	 *
	 * @return The matching enum value.  An empty value will return {@code null}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static SourceType interpret(Object value, SourceType defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}

		if ( SourceType.class.isInstance( value ) ) {
			return (SourceType) value;
		}

		final String name = value.toString().trim();
		if ( name.isEmpty() ) {
			// empty is in fact valid as means to interpret default value based on other settings
			return defaultValue;
		}

		if ( METADATA.externalName.equals( value ) ) {
			return METADATA;
		}
		else if ( SCRIPT.externalName.equals( value ) ) {
			return SCRIPT;
		}
		else if ( METADATA_THEN_SCRIPT.externalName.equals( value ) ) {
			return METADATA_THEN_SCRIPT;
		}
		else if ( SCRIPT_THEN_METADATA.externalName.equals( value ) ) {
			return SCRIPT_THEN_METADATA;
		}

		throw new IllegalArgumentException( "Unrecognized schema generation source-type value : '" + value + '\'');
	}
}
