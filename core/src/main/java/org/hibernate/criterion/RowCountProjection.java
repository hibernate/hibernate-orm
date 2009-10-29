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
package org.hibernate.criterion;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.type.Type;

/**
 * A row count
 *
 * @author Gavin King
 */
public class RowCountProjection extends SimpleProjection {
	private static List ARGS = java.util.Collections.singletonList( "*" );

	public String toString() {
		return "count(*)";
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new Type[] {
				getFunction( criteriaQuery ).getReturnType( null, criteriaQuery.getFactory() )
		};
	}

	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) throws HibernateException {
		return getFunction( criteriaQuery ).render( ARGS, criteriaQuery.getFactory() )
				+ " as y" + position + '_';
	}

	protected SQLFunction getFunction(CriteriaQuery criteriaQuery) {
		SQLFunction function = criteriaQuery.getFactory()
				.getSqlFunctionRegistry()
				.findSQLFunction( "count" );
		if ( function == null ) {
			throw new HibernateException( "Unable to locate count function mapping" );
		}
		return function;
	}
}
