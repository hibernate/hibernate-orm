//$Id: AvgProjection.java 9908 2006-05-08 20:59:20Z max.andersen@jboss.com $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class AvgProjection extends AggregateProjection {

	public AvgProjection(String propertyName) {
		super("avg", propertyName);
	}
	
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return new Type[] { Hibernate.DOUBLE };
	}
}
