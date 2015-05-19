/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
