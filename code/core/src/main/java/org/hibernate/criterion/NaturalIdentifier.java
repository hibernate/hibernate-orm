//$Id: NaturalIdentifier.java 6899 2005-05-25 01:22:07Z oneovthafew $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;

/**
 * @author Gavin King
 */
public class NaturalIdentifier implements Criterion {
		
	private Junction conjunction = new Conjunction();

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return conjunction.getTypedValues(criteria, criteriaQuery);
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return conjunction.toSqlString(criteria, criteriaQuery);
	}
	
	public NaturalIdentifier set(String property, Object value) {
		conjunction.add( Restrictions.eq(property, value) );
		return this;
	}

}
