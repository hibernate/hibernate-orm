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

import org.hibernate.QueryException;
import org.hibernate.Hibernate;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * A Cach&eacute; defintion of a convert function.
 *
 * @author Jonathan Levinson
 */
public class ConvertFunction implements SQLFunction {

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return Hibernate.STRING;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		if ( args.size() != 2 && args.size() != 3 ) {
			throw new QueryException( "convert() requires two or three arguments" );
		}
		String type = ( String ) args.get( 1 );

		if ( args.size() == 2 ) {
			return "{fn convert(" + args.get( 0 ) + " , " + type + ")}";
		}
		else {
			return "convert(" + args.get( 0 ) + " , " + type + "," + args.get( 2 ) + ")";
		}
	}

}
