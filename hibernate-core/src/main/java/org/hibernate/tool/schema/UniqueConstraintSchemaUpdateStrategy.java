/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema;

import java.util.Locale;
import org.jboss.logging.Logger;

/**
 * Unique columns and unique keys both use unique constraints in most dialects.
 * SchemaUpdate needs to create these constraints, but DB's
 * support for finding existing constraints is extremely inconsistent. Further,
 * non-explicitly-named unique constraints use randomly generated characters.
 *
 * @author Brett Meyer
 */
public enum UniqueConstraintSchemaUpdateStrategy {

	/**
	 * Attempt to drop, then (re-)create each unique constraint.  Ignore any
	 * exceptions thrown.  Note that this will require unique keys/constraints
	 * to be explicitly named.  If Hibernate generates the names (randomly),
	 * the drop will not work.
	 *
	 * DEFAULT
	 */
	DROP_RECREATE_QUIETLY,

	/**
	 * Attempt to (re-)create unique constraints, ignoring exceptions thrown
	 * (e.g., if the constraint already existed)
	 */
	RECREATE_QUIETLY,

	/**
	 * Do not attempt to create unique constraints on a schema update
	 */
	SKIP;

	private static final Logger LOG = Logger.getLogger( UniqueConstraintSchemaUpdateStrategy.class );

	public static UniqueConstraintSchemaUpdateStrategy byName(String name) {
		return valueOf( name.toUpperCase(Locale.ROOT) );
	}

	public static UniqueConstraintSchemaUpdateStrategy interpret(Object setting) {
		LOG.tracef( "Interpreting UniqueConstraintSchemaUpdateStrategy from setting: %s", setting );

		if ( setting == null ) {
			// default
			return DROP_RECREATE_QUIETLY;
		}

		if ( setting instanceof UniqueConstraintSchemaUpdateStrategy strategy) {
			return strategy;
		}

		try {
			final UniqueConstraintSchemaUpdateStrategy byName = byName( setting.toString() );
			if ( byName != null ) {
				return byName;
			}
		}
		catch ( Exception ignore ) {
		}

		LOG.debugf( "Unable to interpret given setting [%s] as UniqueConstraintSchemaUpdateStrategy", setting );

		// default
		return DROP_RECREATE_QUIETLY;
	}
}
