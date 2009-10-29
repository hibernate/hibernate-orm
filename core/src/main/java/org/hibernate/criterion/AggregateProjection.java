/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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

	public String toString() {
		return functionName + "(" + propertyName + ')';
	}

	/**
	 * {@inheritDoc}
	 */
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new Type[] {
				getFunction( criteriaQuery ).getReturnType(
						criteriaQuery.getType( criteria, getPropertyName() ),
						criteriaQuery.getFactory()
				)
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public String toSqlString(Criteria criteria, int loc, CriteriaQuery criteriaQuery)
			throws HibernateException {
		final String functionFragment = getFunction( criteriaQuery ).render(
				buildFunctionParameterList( criteria, criteriaQuery ),
				criteriaQuery.getFactory()
		);
		return functionFragment + " as y" + loc + '_';
	}

	protected SQLFunction getFunction(CriteriaQuery criteriaQuery) {
		return getFunction( getFunctionName(), criteriaQuery );
	}

	protected SQLFunction getFunction(String functionName, CriteriaQuery criteriaQuery) {
		SQLFunction function = criteriaQuery.getFactory()
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
}
