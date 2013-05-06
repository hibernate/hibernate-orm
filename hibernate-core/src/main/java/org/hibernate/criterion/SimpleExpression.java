/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.criterion;

import java.sql.Types;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.Type;

/**
 * superclass for "simple" comparisons (with SQL binary operators)
 *
 * @author Gavin King
 */
public class SimpleExpression implements Criterion {
	private final String propertyName;
	private final Object value;
	private boolean ignoreCase;
	private final String op;

	protected SimpleExpression(String propertyName, Object value, String op) {
		this.propertyName = propertyName;
		this.value = value;
		this.op = op;
	}

	protected SimpleExpression(String propertyName, Object value, String op, boolean ignoreCase) {
		this.propertyName = propertyName;
		this.value = value;
		this.ignoreCase = ignoreCase;
		this.op = op;
	}

	protected final String getOp() {
		return op;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Object getValue() {
		return value;
	}

	/**
	 * Make case insensitive.  No effect for non-String values
	 *
	 * @return {@code this}, for method chaining
	 */
	public SimpleExpression ignoreCase() {
		ignoreCase = true;
		return this;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		final Type type = criteriaQuery.getTypeUsingProjection( criteria, propertyName );
		final StringBuilder fragment = new StringBuilder();

		if ( columns.length > 1 ) {
			fragment.append( '(' );
		}
		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final int[] sqlTypes = type.sqlTypes( factory );
		for ( int i = 0; i < columns.length; i++ ) {
			final boolean lower = ignoreCase && (sqlTypes[i] == Types.VARCHAR || sqlTypes[i] == Types.CHAR);
			if ( lower ) {
				fragment.append( factory.getDialect().getLowercaseFunction() ).append( '(' );
			}
			fragment.append( columns[i] );
			if ( lower ) {
				fragment.append( ')' );
			}

			fragment.append( getOp() ).append( "?" );
			if ( i < columns.length - 1 ) {
				fragment.append( " and " );
			}
		}
		if ( columns.length > 1 ) {
			fragment.append( ')' );
		}
		return fragment.toString();
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final Object casedValue = ignoreCase ? value.toString().toLowerCase() : value;
		return new TypedValue[] { criteriaQuery.getTypedValue( criteria, propertyName, casedValue ) };
	}

	@Override
	public String toString() {
		return propertyName + getOp() + value;
	}

}
