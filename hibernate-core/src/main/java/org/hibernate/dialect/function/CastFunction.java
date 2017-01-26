/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.spi.Type;

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
	public Type getReturnType(Type columnType) throws QueryException {
		// this is really just a guess, unless the caller properly identifies the 'type' argument here
		return columnType;
	}

	@Override
	public String render(Type columnType, List args, SessionFactoryImplementor factory) throws QueryException {
		if ( args.size()!=2 ) {
			throw new QueryException( "cast() requires two arguments; found : " + args.size() );
		}
		final String type = (String) args.get( 1 );
		final int jdbcTypeCode = factory.getMetamodel().getTypeConfiguration().resolveCastTargetType( type ).getColumnMapping().getSqlTypeDescriptor().getSqlType();
		String sqlType = factory.getDialect().getCastTypeName( jdbcTypeCode );
		if ( sqlType == null ) {
			//TODO: never reached, since getExplicitHibernateTypeName() actually throws an exception!
			sqlType = type;
		}
		return "cast(" + args.get( 0 ) + " as " + sqlType + ')';
	}

}
