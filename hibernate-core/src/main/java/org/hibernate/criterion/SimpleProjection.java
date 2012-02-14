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
import org.hibernate.type.Type;


/**
 * A single-column projection that may be aliased
 * @author Gavin King
 */
public abstract class SimpleProjection implements EnhancedProjection {

	private static final int NUM_REUSABLE_ALIASES = 40;
	private static final String[] reusableAliases = initializeReusableAliases();

	public Projection as(String alias) {
		return Projections.alias(this, alias);
	}

	private static String[] initializeReusableAliases() {
		String[] aliases = new String[NUM_REUSABLE_ALIASES];
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
			return reusableAliases[loc];
		}
	}

	public String[] getColumnAliases(String alias, int loc) {
		return null;
	}

	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return getColumnAliases( alias, loc );
	}

	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) {
		return null;
	}

	public String[] getColumnAliases(int loc) {
		return new String[] { getAliasForLocation( loc ) };
	}

	public int getColumnCount(Criteria criteria, CriteriaQuery criteriaQuery) {
		Type types[] = getTypes( criteria, criteriaQuery );
		int count = 0;
		for ( int i=0; i<types.length; i++ ) {
			count += types[ i ].getColumnSpan( criteriaQuery.getFactory() );
		}
		return count;
	}

	public String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		int numColumns =  getColumnCount( criteria, criteriaQuery );
		String[] aliases = new String[ numColumns ];
		for (int i = 0; i < numColumns; i++) {
			aliases[i] = getAliasForLocation( loc );
			loc++;
		}
		return aliases;
	}

	public String[] getAliases() {
		return new String[1];
	}

	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		throw new UnsupportedOperationException("not a grouping projection");
	}

	public boolean isGrouped() {
		return false;
	}

}
