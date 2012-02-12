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
package org.hibernate.sql;

import org.hibernate.internal.util.StringHelper;

/**
 * An abstract SQL join fragment renderer
 *
 * @author Gavin King
 */
public abstract class JoinFragment {

	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType);

	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType, String on);

	public abstract void addCrossJoin(String tableName, String alias);

	public abstract void addJoins(String fromFragment, String whereFragment);

	public abstract String toFromFragmentString();

	public abstract String toWhereFragmentString();

	// --Commented out by Inspection (12/4/04 9:10 AM): public abstract void addCondition(String alias, String[] columns, String condition);
	public abstract void addCondition(String alias, String[] fkColumns, String[] pkColumns);

	public abstract boolean addCondition(String condition);
	// --Commented out by Inspection (12/4/04 9:10 AM): public abstract void addFromFragmentString(String fromFragmentString);

	public abstract JoinFragment copy();

	/**
	 * @deprecated use {@link JoinType#INNER_JOIN} instead.
	 */
	@Deprecated
	public static final int INNER_JOIN = 0;
	/**
	 * @deprecated use {@link JoinType#FULL_JOIN} instead.
	 */
	@Deprecated
	public static final int FULL_JOIN = 4;
	/**
	 * @deprecated use {@link JoinType#LEFT_OUTER_JOIN} instead.
	 */
	@Deprecated
	public static final int LEFT_OUTER_JOIN = 1;
	/**
	 * @deprecated use {@link JoinType#RIGHT_OUTER_JOIN} instead.
	 */
	@Deprecated
	public static final int RIGHT_OUTER_JOIN = 2;
	private boolean hasFilterCondition = false;
	private boolean hasThetaJoins = false;

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
			if ( !on.startsWith( " and" ) ) buffer.append( " and " );
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

	public boolean hasThetaJoins() {
		return hasThetaJoins;
	}

	public void setHasThetaJoins(boolean hasThetaJoins) {
		this.hasThetaJoins = hasThetaJoins;
	}
}
