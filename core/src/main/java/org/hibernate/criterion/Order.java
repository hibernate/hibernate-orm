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

import java.io.Serializable;
import java.sql.Types;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Represents an order imposed upon a <tt>Criteria</tt> result set
 * @author Gavin King
 */
public class Order implements Serializable {

	private boolean ascending;
	private boolean ignoreCase;
	private String propertyName;
	
	public String toString() {
		return propertyName + ' ' + (ascending?"asc":"desc");
	}
	
	public Order ignoreCase() {
		ignoreCase = true;
		return this;
	}

	/**
	 * Constructor for Order.
	 */
	protected Order(String propertyName, boolean ascending) {
		this.propertyName = propertyName;
		this.ascending = ascending;
	}

	/**
	 * Render the SQL fragment
	 *
	 */
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		Type type = criteriaQuery.getTypeUsingProjection(criteria, propertyName);
		StringBuffer fragment = new StringBuffer();
		for ( int i=0; i<columns.length; i++ ) {
			SessionFactoryImplementor factory = criteriaQuery.getFactory();
			boolean lower = ignoreCase && type.sqlTypes( factory )[i]==Types.VARCHAR;
			if (lower) {
				fragment.append( factory.getDialect().getLowercaseFunction() )
					.append('(');
			}
			fragment.append( columns[i] );
			if (lower) fragment.append(')');
			fragment.append( ascending ? " asc" : " desc" );
			if ( i<columns.length-1 ) fragment.append(", ");
		}
		return fragment.toString();
	}

	/**
	 * Ascending order
	 *
	 * @param propertyName
	 * @return Order
	 */
	public static Order asc(String propertyName) {
		return new Order(propertyName, true);
	}

	/**
	 * Descending order
	 *
	 * @param propertyName
	 * @return Order
	 */
	public static Order desc(String propertyName) {
		return new Order(propertyName, false);
	}

}
