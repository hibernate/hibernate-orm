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

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class AliasedProjection implements EnhancedProjection {
	
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

	public String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return projection instanceof EnhancedProjection ?
				( ( EnhancedProjection ) projection ).getColumnAliases( loc, criteria, criteriaQuery ) :
				getColumnAliases( loc );
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

	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return this.alias.equals(alias) ?
				getColumnAliases( loc, criteria, criteriaQuery ) :
				null;
	}

	public String[] getAliases() {
		return new String[]{ alias };
	}

	public boolean isGrouped() {
		return projection.isGrouped();
	}

}
