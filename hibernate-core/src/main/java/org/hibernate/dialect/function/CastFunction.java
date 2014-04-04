/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * ANSI-SQL style {@code cast(foo as type)} where the type is a Hibernate type
 *
 * @author Gavin King
 */
public class CastFunction implements SQLFunction {
	/**
	 * Singleton access
	 */
	public static final CastFunction INSTANCE = new CastFunction();

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	@Override
	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		// this is really just a guess, unless the caller properly identifies the 'type' argument here
		return columnType;
	}

	@Override
	public String render(Type columnType, List args, SessionFactoryImplementor factory) throws QueryException {
		if ( args.size()!=2 ) {
			throw new QueryException( "cast() requires two arguments; found : " + args.size() );
		}
		final String type = (String) args.get( 1 );
		final int[] sqlTypeCodes = factory.getTypeResolver().heuristicType( type ).sqlTypes( factory );
		if ( sqlTypeCodes.length!=1 ) {
			throw new QueryException("invalid Hibernate type for cast()");
		}
		String sqlType = factory.getDialect().getCastTypeName( sqlTypeCodes[0] );
		if ( sqlType == null ) {
			//TODO: never reached, since getExplicitHibernateTypeName() actually throws an exception!
			sqlType = type;
		}
		return "cast(" + args.get( 0 ) + " as " + sqlType + ')';
	}

}
