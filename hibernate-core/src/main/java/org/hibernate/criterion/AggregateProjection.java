/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.type.Type;

/**
 * Base class for standard aggregation functions.
 *
 * @author max
 */
public class AggregateProjection extends SimpleProjection {
	protected final String propertyName;
	private final String functionName;
	
	protected AggregateProjection(String functionName, String propertyName) {
		this.functionName = functionName;
		this.propertyName = propertyName;
	}

	public String getFunctionName() {
		return functionName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new Type[] {
				getFunction( criteriaQuery ).getReturnType(
						criteriaQuery.getType( criteria, getPropertyName() ),
						criteriaQuery.getFactory()
				)
		};
	}

	@Override
	public String toSqlString(Criteria criteria, int loc, CriteriaQuery criteriaQuery) throws HibernateException {
		final String functionFragment = getFunction( criteriaQuery ).render(
				criteriaQuery.getType( criteria, getPropertyName() ),
				buildFunctionParameterList( criteria, criteriaQuery ),
				criteriaQuery.getFactory()
		);
		return functionFragment + " as y" + loc + '_';
	}

	protected SQLFunction getFunction(CriteriaQuery criteriaQuery) {
		return getFunction( getFunctionName(), criteriaQuery );
	}

	protected SQLFunction getFunction(String functionName, CriteriaQuery criteriaQuery) {
		final SQLFunction function = criteriaQuery.getFactory()
				.getSqlFunctionRegistry()
				.findSQLFunction( functionName );
		if ( function == null ) {
			throw new HibernateException( "Unable to locate mapping for function named [" + functionName + "]" );
		}
		return function;
	}

	protected List buildFunctionParameterList(Criteria criteria, CriteriaQuery criteriaQuery) {
		return buildFunctionParameterList( criteriaQuery.getColumn( criteria, getPropertyName() ) );
	}

	protected List buildFunctionParameterList(String column) {
		return Collections.singletonList( column );
	}

	@Override
	public String toString() {
		return functionName + "(" + propertyName + ')';
	}

}
