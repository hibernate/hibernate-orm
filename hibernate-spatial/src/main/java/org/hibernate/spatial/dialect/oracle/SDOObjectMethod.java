/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Special SQLFunction implementation for Oracle object methods
 *
 * @author Karel Maesen
 */
class SDOObjectMethod implements SQLFunction {

	private final Type type;

	private final String name;

	public SDOObjectMethod(String name, Type type) {
		this.type = type;
		this.name = name;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.dialect.function.SQLFunction#getReturnType(org.hibernate.type.Type,
		  *      org.hibernate.engine.Mapping)
		  */

	public Type getReturnType(Type columnType, Mapping mapping)
			throws QueryException {
		return type == null ? columnType : type;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.dialect.function.SQLFunction#hasArguments()
		  */

	public boolean hasArguments() {
		return true;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.dialect.function.SQLFunction#hasParenthesesIfNoArguments()
		  */

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String getName() {
		return this.name;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.dialect.function.SQLFunction#render(java.util.List,
		  *      org.hibernate.engine.SessionFactoryImplementor)
		  */

	public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		final StringBuffer buf = new StringBuffer();
		if ( args.isEmpty() ) {
			throw new QueryException(
					"First Argument in arglist must be object to which method is applied"
			);
		}
		buf.append( args.get( 0 ) ).append( "." ).append( name ).append( '(' );
		for ( int i = 1; i < args.size(); i++ ) {
			buf.append( args.get( i ) );
			if ( i < args.size() - 1 ) {
				buf.append( ", " );
			}
		}
		return buf.append( ')' ).toString();
	}

}
