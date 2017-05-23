/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
