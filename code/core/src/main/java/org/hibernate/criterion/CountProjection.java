//$Id: CountProjection.java 9913 2006-05-09 07:40:11Z max.andersen@jboss.com $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * A count
 * @author Gavin King
 */
public class CountProjection extends AggregateProjection {

	private boolean distinct;

	protected CountProjection(String prop) {
		super("count", prop);
	}

	public String toString() {
		if(distinct) {
			return "distinct " + super.toString();
		} else {
			return super.toString();
		}
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return new Type[] { Hibernate.INTEGER };
	}

	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		StringBuffer buf = new StringBuffer();
		buf.append("count(");
		if (distinct) buf.append("distinct ");
		return buf.append( criteriaQuery.getColumn(criteria, propertyName) )
			.append(") as y")
			.append(position)
			.append('_')
			.toString();
	}
	
	public CountProjection setDistinct() {
		distinct = true;
		return this;
	}
	
}
