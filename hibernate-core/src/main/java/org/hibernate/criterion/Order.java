/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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

import java.io.Serializable;
import java.sql.Types;

import org.hibernate.Criteria;
import org.hibernate.NullPrecedence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Represents an ordering imposed upon the results of a Criteria
 * 
 * @author Gavin King
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class Order implements Serializable {
	private boolean ascending;
	private boolean ignoreCase;
	private String propertyName;
	private NullPrecedence nullPrecedence;

	/**
	 * Ascending order
	 *
	 * @param propertyName The property to order on
	 *
	 * @return The build Order instance
	 */
	public static Order asc(String propertyName) {
		return new Order( propertyName, true );
	}

	/**
	 * Descending order.
	 *
	 * @param propertyName The property to order on
	 *
	 * @return The build Order instance
	 */
	public static Order desc(String propertyName) {
		return new Order( propertyName, false );
	}

	/**
	 * Constructor for Order.  Order instances are generally created by factory methods.
	 *
	 * @see #asc
	 * @see #desc
	 */
	protected Order(String propertyName, boolean ascending) {
		this.propertyName = propertyName;
		this.ascending = ascending;
	}

	/**
	 * Should this ordering ignore case?  Has no effect on non-character properties.
	 *
	 * @return {@code this}, for method chaining
	 */
	public Order ignoreCase() {
		ignoreCase = true;
		return this;
	}

	/**
	 * Defines precedence for nulls.
	 *
	 * @param nullPrecedence The null precedence to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public Order nulls(NullPrecedence nullPrecedence) {
		this.nullPrecedence = nullPrecedence;
		return this;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isAscending() {
		return ascending;
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isIgnoreCase() {
		return ignoreCase;
	}


	/**
	 * Render the SQL fragment
	 *
	 * @param criteria The criteria
	 * @param criteriaQuery The overall query
	 *
	 * @return The ORDER BY fragment for this ordering
	 */
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, propertyName );
		final Type type = criteriaQuery.getTypeUsingProjection( criteria, propertyName );
		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final int[] sqlTypes = type.sqlTypes( factory );

		final StringBuilder fragment = new StringBuilder();
		for ( int i=0; i<columns.length; i++ ) {
			final StringBuilder expression = new StringBuilder();
			boolean lower = false;
			if ( ignoreCase ) {
				final int sqlType = sqlTypes[i];
				lower = sqlType == Types.VARCHAR
						|| sqlType == Types.CHAR
						|| sqlType == Types.LONGVARCHAR;
			}
			
			if ( lower ) {
				expression.append( factory.getDialect().getLowercaseFunction() )
						.append( '(' );
			}
			expression.append( columns[i] );
			if ( lower ) {
				expression.append( ')' );
			}

			fragment.append(
					factory.getDialect().renderOrderByElement(
							expression.toString(),
							null,
							ascending ? "asc" : "desc",
							nullPrecedence != null ? nullPrecedence : factory.getSettings().getDefaultNullPrecedence()
					)
			);
			if ( i < columns.length-1 ) {
				fragment.append( ", " );
			}
		}

		return fragment.toString();
	}
	
	@Override
	public String toString() {
		return propertyName + ' '
				+ ( ascending ? "asc" : "desc" )
				+ ( nullPrecedence != null ? ' ' + nullPrecedence.name().toLowerCase() : "" );
	}
}
