//$Id: AggregateProjection.java 9908 2006-05-08 20:59:20Z max.andersen@jboss.com $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * An aggregation
 * @author max
 */
public class AggregateProjection extends SimpleProjection {

	protected final String propertyName;
	private final String aggregate;
	
	protected AggregateProjection(String aggregate, String propertyName) {
		this.aggregate = aggregate;
		this.propertyName = propertyName;
	}

	public String toString() {
		return aggregate + "(" + propertyName + ')';
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return new Type[] { criteriaQuery.getType(criteria, propertyName) };
	}

	public String toSqlString(Criteria criteria, int loc, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return new StringBuffer()
			.append(aggregate)
			.append("(")
			.append( criteriaQuery.getColumn(criteria, propertyName) )
			.append(") as y")
			.append(loc)
			.append('_')
			.toString();
	}

}
