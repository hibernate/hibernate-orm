//$Id: IdentifierProjection.java 6970 2005-05-31 20:24:41Z oneovthafew $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * A property value, or grouped property value
 * @author Gavin King
 */
public class IdentifierProjection extends SimpleProjection {

	private boolean grouped;
	
	protected IdentifierProjection(boolean grouped) {
		this.grouped = grouped;
	}
	
	protected IdentifierProjection() {
		this(false);
	}
	
	public String toString() {
		return "id";
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return new Type[] { criteriaQuery.getIdentifierType(criteria) };
	}

	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		StringBuffer buf = new StringBuffer();
		String[] cols = criteriaQuery.getIdentifierColumns(criteria);
		for ( int i=0; i<cols.length; i++ ) {
			buf.append( cols[i] )
				.append(" as y")
				.append(position + i)
				.append('_');
		}
		return buf.toString();
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
			return StringHelper.join( ", ", criteriaQuery.getIdentifierColumns(criteria) );
		}
	}

}
