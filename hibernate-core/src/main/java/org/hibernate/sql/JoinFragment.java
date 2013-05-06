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
package org.hibernate.sql;

import org.hibernate.internal.util.StringHelper;

/**
 * An abstract SQL join fragment renderer
 *
 * @author Gavin King
 */
public abstract class JoinFragment {
	/**
	 * Specifies an inner join.
	 *
	 * @deprecated use {@link JoinType#INNER_JOIN} instead.
	 */
	@Deprecated
	public static final int INNER_JOIN = JoinType.INNER_JOIN.getJoinTypeValue();

	/**
	 * Specifies a full join
	 *
	 * @deprecated use {@link JoinType#FULL_JOIN} instead.
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	public static final int FULL_JOIN = JoinType.FULL_JOIN.getJoinTypeValue();

	/**
	 * Specifies a left join.
	 *
	 * @deprecated use {@link JoinType#LEFT_OUTER_JOIN} instead.
	 */
	@Deprecated
	public static final int LEFT_OUTER_JOIN = JoinType.LEFT_OUTER_JOIN.getJoinTypeValue();

	/**
	 * Specifies a right join.
	 *
	 * @deprecated use {@link JoinType#RIGHT_OUTER_JOIN} instead.
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	public static final int RIGHT_OUTER_JOIN = JoinType.RIGHT_OUTER_JOIN.getJoinTypeValue();


	private boolean hasFilterCondition;
	private boolean hasThetaJoins;


	/**
	 * Adds a join.
	 *
	 * @param tableName The name of the table to be joined
	 * @param alias The alias to apply to the joined table
	 * @param fkColumns The names of the columns which reference the joined table
	 * @param pkColumns The columns in the joined table being referenced
	 * @param joinType The type of join
	 */
	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType);

	/**
	 * Adds a join, with an additional ON clause fragment
	 *
	 * @param tableName The name of the table to be joined
	 * @param alias The alias to apply to the joined table
	 * @param fkColumns The names of the columns which reference the joined table
	 * @param pkColumns The columns in the joined table being referenced
	 * @param joinType The type of join
	 * @param on The additional ON fragment
	 */
	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType, String on);

	/**
	 * Adds a cross join to the specified table.
	 *
	 * @param tableName The name of the table to be joined
	 * @param alias The alias to apply to the joined table
	 */
	public abstract void addCrossJoin(String tableName, String alias);

	/**
	 * Free-form form of adding theta-style joins taking the necessary FROM and WHERE clause fragments
	 *
	 * @param fromFragment The FROM clause fragment
	 * @param whereFragment The WHERE clause fragment
	 */
	public abstract void addJoins(String fromFragment, String whereFragment);

	/**
	 * Render this fragment to its FROM clause portion
	 *
	 * @return The FROM clause portion of this fragment
	 */
	public abstract String toFromFragmentString();

	/**
	 * Render this fragment to its WHERE clause portion
	 *
	 * @return The WHERE clause portion of this fragment
	 */
	public abstract String toWhereFragmentString();

	/**
	 * Adds a condition to the join fragment.
	 *
	 * @param alias The alias of the joined table
	 * @param fkColumns The names of the columns which reference the joined table
	 * @param pkColumns The columns in the joined table being referenced
	 */
	public abstract void addCondition(String alias, String[] fkColumns, String[] pkColumns);

	/**
	 * Adds a free-form condition fragment
	 *
	 * @param condition The fragment
	 *
	 * @return {@code true} if the condition was added
	 */
	public abstract boolean addCondition(String condition);

	/**
	 * Make a copy.
	 *
	 * @return The copy.
	 */
	public abstract JoinFragment copy();

	/**
	 * Adds another join fragment to this one.
	 *
	 * @param ojf The other join fragment
	 */
	public void addFragment(JoinFragment ojf) {
		if ( ojf.hasThetaJoins() ) {
			hasThetaJoins = true;
		}
		addJoins( ojf.toFromFragmentString(), ojf.toWhereFragmentString() );
	}

	/**
	 * Appends the 'on' condition to the buffer, returning true if the condition was added.
	 * Returns false if the 'on' condition was empty.
	 *
	 * @param buffer The buffer to append the 'on' condition to.
	 * @param on     The 'on' condition.
	 * @return Returns true if the condition was added, false if the condition was already in 'on' string.
	 */
	protected boolean addCondition(StringBuilder buffer, String on) {
		if ( StringHelper.isNotEmpty( on ) ) {
			if ( !on.startsWith( " and" ) ) {
				buffer.append( " and " );
			}
			buffer.append( on );
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * True if the where fragment is from a filter condition.
	 *
	 * @return True if the where fragment is from a filter condition.
	 */
	public boolean hasFilterCondition() {
		return hasFilterCondition;
	}

	public void setHasFilterCondition(boolean b) {
		this.hasFilterCondition = b;
	}

	/**
	 * Determine if the join fragment contained any theta-joins.
	 *
	 * @return {@code true} if the fragment contained theta joins
	 */
	public boolean hasThetaJoins() {
		return hasThetaJoins;
	}

	public void setHasThetaJoins(boolean hasThetaJoins) {
		this.hasThetaJoins = hasThetaJoins;
	}
}
