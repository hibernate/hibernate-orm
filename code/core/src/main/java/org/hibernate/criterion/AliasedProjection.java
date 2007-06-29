//$Id: AliasedProjection.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class AliasedProjection implements Projection {
	
	private final Projection projection;
	private final String alias;
	
	public String toString() {
		return projection.toString() + " as " + alias;
	}
	
	protected AliasedProjection(Projection projection, String alias) {
		this.projection = projection;
		this.alias = alias;
	}

	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return projection.toSqlString(criteria, position, criteriaQuery);
	}

	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return projection.toGroupSqlString(criteria, criteriaQuery);
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return projection.getTypes(criteria, criteriaQuery);
	}

	public String[] getColumnAliases(int loc) {
		return projection.getColumnAliases(loc);
	}

	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return this.alias.equals(alias) ?
				getTypes(criteria, criteriaQuery) :
				null;
	}

	public String[] getColumnAliases(String alias, int loc) {
		return this.alias.equals(alias) ? 
				getColumnAliases(loc) :
				null;
	}

	public String[] getAliases() {
		return new String[]{ alias };
	}

	public boolean isGrouped() {
		return projection.isGrouped();
	}

}
