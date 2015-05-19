/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;
import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;

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
