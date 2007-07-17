//$Id: PropertyProjection.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * A property value, or grouped property value
 * @author Gavin King
 */
public class PropertyProjection extends SimpleProjection {

	private String propertyName;
	private boolean grouped;
	
	protected PropertyProjection(String prop, boolean grouped) {
		this.propertyName = prop;
		this.grouped = grouped;
	}
	
	protected PropertyProjection(String prop) {
		this(prop, false);
	}

	public String getPropertyName() {
		return propertyName;
	}
	
	public String toString() {
		return propertyName;
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return new Type[] { criteriaQuery.getType(criteria, propertyName) };
	}

	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return new StringBuffer()
			.append( criteriaQuery.getColumn(criteria, propertyName) )
			.append(" as y")
			.append(position)
			.append('_')
			.toString();
	}

	public boolean isGrouped() {
		return grouped;
	}
	
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		if (!grouped) {
			return super.toGroupSqlString(criteria, criteriaQuery);
		}
		else {
			return criteriaQuery.getColumn(criteria, propertyName);
		}
	}

}
