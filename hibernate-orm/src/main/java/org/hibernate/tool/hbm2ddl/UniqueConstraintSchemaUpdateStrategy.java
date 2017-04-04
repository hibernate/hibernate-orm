/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

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

	private static final Logger log = Logger.getLogger( UniqueConstraintSchemaUpdateStrategy.class );

	public static UniqueConstraintSchemaUpdateStrategy byName(String name) {
		return valueOf( name.toUpperCase(Locale.ROOT) );
	}

	public static UniqueConstraintSchemaUpdateStrategy interpret(Object setting) {
		log.tracef( "Interpreting UniqueConstraintSchemaUpdateStrategy from setting : %s", setting );

		if ( setting == null ) {
			// default
			return DROP_RECREATE_QUIETLY;
		}

		if ( UniqueConstraintSchemaUpdateStrategy.class.isInstance( setting ) ) {
			return (UniqueConstraintSchemaUpdateStrategy) setting;
		}

		try {
			final UniqueConstraintSchemaUpdateStrategy byName = byName( setting.toString() );
			if ( byName != null ) {
				return byName;
			}
		}
		catch ( Exception ignore ) {
		}

		log.debugf( "Unable to interpret given setting [%s] as UniqueConstraintSchemaUpdateStrategy", setting );

		// default
		return DROP_RECREATE_QUIETLY;
	}
}
