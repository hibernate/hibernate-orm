/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.type.Type;

/**
 * A wrappedProjection that is a wrapper around other projections to apply distinction.
 *
 * @author Gavin King
 */
public class Distinct implements EnhancedProjection {
	private final Projection wrappedProjection;

	/**
	 * Constructs a Distinct
	 *
	 * @param wrappedProjection The wrapped projection
	 */
	public Distinct(Projection wrappedProjection) {
		this.wrappedProjection = wrappedProjection;
	}

	@Override
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) {
		return "distinct " + wrappedProjection.toSqlString( criteria, position, criteriaQuery );
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return wrappedProjection.toGroupSqlString( criteria, criteriaQuery );
	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) {
		return wrappedProjection.getTypes( criteria, criteriaQuery );
	}

	@Override
	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) {
		return wrappedProjection.getTypes( alias, criteria, criteriaQuery );
	}

	@Override
	public String[] getColumnAliases(int loc) {
		return wrappedProjection.getColumnAliases( loc );
	}

	@Override
	public String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return wrappedProjection instanceof EnhancedProjection
				? ( (EnhancedProjection) wrappedProjection).getColumnAliases( loc, criteria, criteriaQuery )
				: getColumnAliases( loc );
	}

	@Override
	public String[] getColumnAliases(String alias, int loc) {
		return wrappedProjection.getColumnAliases( alias, loc );
	}

	@Override
	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return wrappedProjection instanceof EnhancedProjection
				? ( (EnhancedProjection) wrappedProjection).getColumnAliases( alias, loc, criteria, criteriaQuery )
				: getColumnAliases( alias, loc );
	}

	@Override
	public String[] getAliases() {
		return wrappedProjection.getAliases();
	}

	@Override
	public boolean isGrouped() {
		return wrappedProjection.isGrouped();
	}

	@Override
	public String toString() {
		return "distinct " + wrappedProjection.toString();
	}
}
