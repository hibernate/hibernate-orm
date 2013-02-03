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
 * Describes the allowable values of the {@value AvailableSettings#SCHEMA_GEN_SOURCE} setting.
 *
 * @see AvailableSettings#SCHEMA_GEN_SOURCE
 *
 * @author Steve Ebersole
 */
public enum SchemaGenSource {
	/**
	 * "metadata" - The O/RM metadata is used as the exclusive source for generation
	 */
	METADATA( "metadata" ),
	/**
	 * "scripts" - External DDL script(s) are used as the exclusive source for generation.  The scripts for schema
	 * creation and dropping come from different sources.  The creation DDL script is identified by the
	 * {@value AvailableSettings#SCHEMA_GEN_CREATE_SCRIPT_SOURCE} setting; the drop DDL script is identified by the
	 * {@value AvailableSettings#SCHEMA_GEN_DROP_SCRIPT_SOURCE} setting.
	 *
	 * @see AvailableSettings#SCHEMA_GEN_CREATE_SCRIPT_SOURCE
	 * @see AvailableSettings#SCHEMA_GEN_DROP_SCRIPT_SOURCE
	 */
	SCRIPTS( "scripts" ),
	/**
	 * "metadata-then-scripts" - Both the O/RM metadata and external DDL scripts are used as sources for generation,
	 * with the O/RM metadata being applied first.
	 *
	 * @see #METADATA
	 * @see #SCRIPTS
	 */
	METADATA_THEN_SCRIPTS( "metadata-then-scripts" ),
	/**
	 * "scripts-then-metadata" - Both the O/RM metadata and external DDL scripts are used as sources for generation,
	 * with the commands from the external DDL script(s) being applied first
	 *
	 * @see #SCRIPTS
	 * @see #METADATA
	 */
	SCRIPTS_THEN_METADATA( "scripts-then-metadata" );

	private final String externalName;

	private SchemaGenSource(String externalName) {
		this.externalName = externalName;
	}

	/**
	 * Used when processing JPA configuration to interpret the {@value AvailableSettings#SCHEMA_GEN_SOURCE} setting.
	 *
	 * @param value The encountered value of {@value AvailableSettings#SCHEMA_GEN_SOURCE}
	 *
	 * @return The matching enum value.  An empty value will return {@code null}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static SchemaGenSource interpret(String value) {
		if ( StringHelper.isEmpty( value ) ) {
			// empty is in fact valid as means to interpret default value based on other settings
			return null;
		}

		if ( METADATA.externalName.equals( value ) ) {
			return METADATA;
		}
		else if ( SCRIPTS.externalName.equals( value ) ) {
			return SCRIPTS;
		}
		else if ( METADATA_THEN_SCRIPTS.externalName.equals( value ) ) {
			return METADATA_THEN_SCRIPTS;
		}
		else if ( SCRIPTS_THEN_METADATA.externalName.equals( value ) ) {
			return SCRIPTS_THEN_METADATA;
		}

		throw new IllegalArgumentException( "Unrecognized schema generation source value : " + value );
	}
}
