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
 * A single-column projection that may be aliased
 *
 * @author Gavin King
 */
public abstract class SimpleProjection implements EnhancedProjection {
	private static final int NUM_REUSABLE_ALIASES = 40;
	private static final String[] REUSABLE_ALIASES = initializeReusableAliases();

	private static String[] initializeReusableAliases() {
		final String[] aliases = new String[NUM_REUSABLE_ALIASES];
		for ( int i = 0; i < NUM_REUSABLE_ALIASES; i++ ) {
			aliases[i] = aliasForLocation( i );
		}
		return aliases;
	}

	private static String aliasForLocation(final int loc) {
		return "y" + loc + "_";
	}

	private static String getAliasForLocation(final int loc) {
		if ( loc >= NUM_REUSABLE_ALIASES ) {
			return aliasForLocation( loc );
		}
		else {
			return REUSABLE_ALIASES[loc];
		}
	}

	/**
	 * Create an aliased form of this projection
	 *
	 * @param alias The alias to apply
	 *
	 * @return The aliased projection
	 */
	public Projection as(String alias) {
		return Projections.alias( this, alias );
	}

	@Override
	public String[] getColumnAliases(String alias, int loc) {
		return null;
	}

	@Override
	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return getColumnAliases( alias, loc );
	}

	@Override
	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) {
		return null;
	}

	@Override
	public String[] getColumnAliases(int loc) {
		return new String[] { getAliasForLocation( loc ) };
	}

	/**
	 * Count the number of columns this projection uses.
	 *
	 * @param criteria The criteria
	 * @param criteriaQuery The query
	 *
	 * @return The number of columns
	 */
	public int getColumnCount(Criteria criteria, CriteriaQuery criteriaQuery) {
		final Type[] types = getTypes( criteria, criteriaQuery );
		int count = 0;
		for ( Type type : types ) {
			count += type.getColumnSpan( criteriaQuery.getFactory() );
		}
		return count;
	}

	@Override
	public String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		final int numColumns =  getColumnCount( criteria, criteriaQuery );
		final String[] aliases = new String[ numColumns ];
		for (int i = 0; i < numColumns; i++) {
			aliases[i] = getAliasForLocation( loc );
			loc++;
		}
		return aliases;
	}

	@Override
	public String[] getAliases() {
		return new String[1];
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		throw new UnsupportedOperationException( "not a grouping projection" );
	}

	@Override
	public boolean isGrouped() {
		return false;
	}

}
