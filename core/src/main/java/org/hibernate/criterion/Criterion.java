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
import org.hibernate.engine.TypedValue;

/**
 * An object-oriented representation of a query criterion that may be used 
 * as a restriction in a <tt>Criteria</tt> query.
 * Built-in criterion types are provided by the <tt>Restrictions</tt> factory 
 * class. This interface might be implemented by application classes that 
 * define custom restriction criteria.
 *
 * @see Restrictions
 * @see Criteria
 * @author Gavin King
 */
public interface Criterion extends Serializable {

	/**
	 * Render the SQL fragment
	 *
	 * @param criteria The local criteria
	 * @param criteriaQuery The overal criteria query
	 *
	 * @return The generated SQL fragment
	 * @throws org.hibernate.HibernateException Problem during rendering.
	 */
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException;
	
	/**
	 * Return typed values for all parameters in the rendered SQL fragment
	 *
	 * @param criteria The local criteria
	 * @param criteriaQuery The overal criteria query
	 *
	 * @return The types values (for binding)
	 * @throws HibernateException Problem determining types.
	 */
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException;

}
