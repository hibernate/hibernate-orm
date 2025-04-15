/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema;

import org.hibernate.cfg.AvailableSettings;

/**
 * Enumerates the various types of sources understood by the schema management
 * tooling.
 *
 * @see AvailableSettings#JAKARTA_HBM2DDL_CREATE_SOURCE
 * @see AvailableSettings#JAKARTA_HBM2DDL_DROP_SOURCE
 *
 * @author Steve Ebersole
 */
public enum SourceType {
	/**
	 * The ORM metadata is used as the exclusive source generation of for DDL
	 * commands.
	 */
	METADATA,
	/**
	 * External DDL script(s) are used as the exclusive source for DDL commands.
	 * <p>
	 * The scripts for schema creation and dropping come from different sources:
	 * <ul>
	 * <li>The creation DDL script is identified by the setting
	 * {@value AvailableSettings#JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE}.
	 * <li>The drop DDL script is identified by the setting
	 * {@value AvailableSettings#JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE}.
	 * </ul>
	 *
	 * @see AvailableSettings#JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE
	 * @see AvailableSettings#JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE
	 */
	SCRIPT,
	/**
	 * Both the ORM metadata and external DDL scripts are used as sources for
	 * generation, with the commands from the external DDL script(s) being
	 * executed last.
	 *
	 * @see #METADATA
	 * @see #SCRIPT
	 */
	METADATA_THEN_SCRIPT,
	/**
	 * Both the O/M metadata and external DDL scripts are used as sources for
	 * generation, with the commands from the external DDL script(s) being
	 * executed first.
	 *
	 * @see #SCRIPT
	 * @see #METADATA
	 */
	SCRIPT_THEN_METADATA;

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

		if ( value instanceof SourceType sourceType ) {
			return sourceType;
		}

		final String name = value.toString().trim().replace('-', '_');
		if ( name.isEmpty() ) {
			return METADATA;
		}
		for ( SourceType sourceType: values() ) {
			if ( sourceType.toString().equalsIgnoreCase(name) ) {
				return sourceType;
			}
		}
		throw new IllegalArgumentException( "Unrecognized schema generation source type: '" + value + "'");
	}
}
