/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.tool.hbm2ddl;

import org.jboss.logging.Logger;

/**
 * Unique columns and unique keys both use unique constraints in most dialects.
 * SchemaUpdate needs to create these constraints, but DB's
 * support for finding existing constraints is extremely inconsistent. Further,
 * non-explicitly-named unique constraints use randomly generated characters.
 * 
 * Therefore, allow users to select from these strategies.
 * {@link #RECREATE_QUIETLY} (DEFAULT): attempt to (re-)create all unique constraints,
 * 			ignoring exceptions throw if the constraint already existed
 * {@link #SKIP}: do not attempt to create unique constraints on a schema update
 * 
 * @author Brett Meyer
 */
public enum UniqueConstraintSchemaUpdateStrategy {
	
	/**
	 * Attempt to drop, then (re-)create each unique constraint.
	 * Ignore any exceptions thrown.  Note
	 * that this will require unique keys/constraints to be explicitly named.
	 * If Hibernate generates the names (randomly), the drop will not work.
	 * 
	 * DEFAULT
	 */
	DROP_RECREATE_QUIETLY,
	
	/**
	 * Attempt to (re-)create unique constraints,
	 * ignoring exceptions thrown if the constraint already existed
	 */
	RECREATE_QUIETLY,

	/**
	 * Do not attempt to create unique constraints on a schema update
	 */
	SKIP;

	private static final Logger log = Logger.getLogger( UniqueConstraintSchemaUpdateStrategy.class );

	public static UniqueConstraintSchemaUpdateStrategy byName(String name) {
		return valueOf( name.toUpperCase() );
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
