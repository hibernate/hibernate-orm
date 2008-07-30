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

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * An object-oriented representation of a query result set projection 
 * in a <tt>Criteria</tt> query. Built-in projection types are provided 
 * by the <tt>Projections</tt> factory class.
 * This interface might be implemented by application classes that
 * define custom projections.
 *
 * @see Projections
 * @see org.hibernate.Criteria
 * @author Gavin King
 */
public interface Projection extends Serializable {

	/**
	 * Render the SQL fragment
	 * @param criteriaQuery
	 * @param columnAlias
	 * @return String
	 * @throws HibernateException
	 */
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) 
	throws HibernateException;
	
	/**
	 * Render the SQL fragment to be used in the group by clause
	 * @param criteriaQuery
	 * @param columnAlias
	 * @return String
	 * @throws HibernateException
	 */
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException;
	
	/**
	 * Return types returned by the rendered SQL fragment
	 * @param criteria
	 * @param criteriaQuery 
	 * @return Type[]
	 * @throws HibernateException
	 */
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException;
	/**
	 * Return types for a particular user-visible alias
	 */
	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException;
		
	/**
	 * Get the SQL select clause column aliases
	 */
	public String[] getColumnAliases(int loc);
	/**
	 * Get the SQL select clause column aliases for a particular
	 * user-visible alias
	 */
	public String[] getColumnAliases(String alias, int loc);
	
	/**
	 * Get the user-visible aliases for this projection
	 * (ie. the ones that will be passed to the 
	 * <tt>ResultTransformer</tt>)
	 */
	public String[] getAliases();
	
	/**
	 * Does this projection specify grouping attributes?
	 */
	public boolean isGrouped();
	
}
