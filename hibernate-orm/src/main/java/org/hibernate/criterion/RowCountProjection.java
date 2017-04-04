/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.type.Type;

/**
 * A row count
 *
 * @author Gavin King
 */
public class RowCountProjection extends SimpleProjection {
	private static final List ARGS = java.util.Collections.singletonList( "*" );

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final Type countFunctionReturnType = getFunction( criteriaQuery ).getReturnType( null, criteriaQuery.getFactory() );
		return new Type[] { countFunctionReturnType };
	}

	@Override
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) throws HibernateException {
		return getFunction( criteriaQuery ).render( null, ARGS, criteriaQuery.getFactory() ) + " as y" + position + '_';
	}

	protected SQLFunction getFunction(CriteriaQuery criteriaQuery) {
		final SQLFunctionRegistry sqlFunctionRegistry = criteriaQuery.getFactory().getSqlFunctionRegistry();
		final SQLFunction function = sqlFunctionRegistry.findSQLFunction( "count" );
		if ( function == null ) {
			throw new HibernateException( "Unable to locate count function mapping" );
		}
		return function;
	}

	@Override
	public String toString() {
		return "count(*)";
	}
}
