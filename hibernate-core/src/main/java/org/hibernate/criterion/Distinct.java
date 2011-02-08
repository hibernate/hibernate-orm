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
public class Distinct implements EnhancedProjection {

	private final Projection projection;
	
	public Distinct(Projection proj) {
		this.projection = proj;
	}

	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery)
			throws HibernateException {
		return "distinct " + projection.toSqlString(criteria, position, criteriaQuery);
	}

	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
			throws HibernateException {
		return projection.toGroupSqlString(criteria, criteriaQuery);
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery)
			throws HibernateException {
		return projection.getTypes(criteria, criteriaQuery);
	}

	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery)
			throws HibernateException {
		return projection.getTypes(alias, criteria, criteriaQuery);
	}

	public String[] getColumnAliases(int loc) {
		return projection.getColumnAliases(loc);
	}

	public String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return projection instanceof EnhancedProjection ?
				( ( EnhancedProjection ) projection ).getColumnAliases( loc, criteria, criteriaQuery ) :
				getColumnAliases( loc );
	}

	public String[] getColumnAliases(String alias, int loc) {
		return projection.getColumnAliases(alias, loc);
	}

	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return projection instanceof EnhancedProjection ?
				( ( EnhancedProjection ) projection ).getColumnAliases( alias, loc, criteria, criteriaQuery ) :
				getColumnAliases( alias, loc );
	}

	public String[] getAliases() {
		return projection.getAliases();
	}

	public boolean isGrouped() {
		return projection.isGrouped();
	}

	public String toString() {
		return "distinct " + projection.toString();
	}
}
