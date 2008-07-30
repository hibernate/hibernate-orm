/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 */
public class StandardSQLFunction implements SQLFunction {
	private final String name;
	private final Type type;

	/**
	 * Construct a standard SQL function definition with a variable return type;
	 * the actual return type will depend on the types to which the function
	 * is applied.
	 * <p/>
	 * Using this form, the return type is considered non-static and assumed
	 * to be the type of the first argument.
	 *
	 * @param name The name of the function.
	 */
	public StandardSQLFunction(String name) {
		this( name, null );
	}

	/**
	 * Construct a standard SQL function definition with a static return type.
	 *
	 * @param name The name of the function.
	 * @param type The static return type.
	 */
	public StandardSQLFunction(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	/**
	 * Function name accessor
	 *
	 * @return The function name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Function static return type accessor.
	 *
	 * @return The static function return type; or null if return type is
	 * not static.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getReturnType(Type columnType, Mapping mapping) {
		// return the concrete type, or the underlying type if a concrete type
		// was not specified
		return type == null ? columnType : type;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String render(List args, SessionFactoryImplementor factory) {
		StringBuffer buf = new StringBuffer();
		buf.append( name ).append( '(' );
		for ( int i = 0; i < args.size(); i++ ) {
			buf.append( args.get( i ) );
			if ( i < args.size() - 1 ) {
				buf.append( ", " );
			}
		}
		return buf.append( ')' ).toString();
	}

	public String toString() {
		return name;
	}
}
