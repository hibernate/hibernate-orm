/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

/**
 * Represents a join in the QuerySpace-sense.  In HQL/JP-QL, this would be an implicit/explicit join; in
 * metamodel-driven LoadPlans, this would be joins indicated by the metamodel.
 */
public interface Join {
	/**
	 * Get the {@link QuerySpace} from the left-hand-side of the join.
	 *
	 * @return the query space from the left-hand-side of the join.
	 */
	public QuerySpace getLeftHandSide();

	/**
	 * Get the {@link QuerySpace} from the right-hand-side of the join.
	 *
	 * @return the query space from the right-hand-side of the join.
	 */
	public QuerySpace getRightHandSide();

	/**
	 * Indicates if the joined attribute is required to be non-null.
	 *
	 * @return true, if the joined attribute is required to be non-null; false, otherwise.
	 */
	public boolean isRightHandSideRequired();

	// Ugh!  This part will unfortunately be SQL specific :( ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Resolves the column names prefixed by the specified alias on the
	 * left-hand-side of the join.
	 *
	 * @param leftHandSideTableAlias The table alias used to prefix the columns.
	 * @return the aliased columns on the left-hand-side of the join.
	 */
	public String[] resolveAliasedLeftHandSideJoinConditionColumns(String leftHandSideTableAlias);

	/**
	 * Resolves the raw (unaliased) column names on the right-hand-side of the join.
	 *
	 * @return the columns on the right-hand-side of the join.
	 */
	public String[] resolveNonAliasedRightHandSideJoinConditionColumns();

	/**
	 * Gets any additional conditions on the right-hand-side of the join using
	 * the specified table alias.
	 *
	 * @param rhsTableAlias The table alias.
	 * @return additional conditions on the right-hand-side of the join.
	 */
	public String getAnyAdditionalJoinConditions(String rhsTableAlias);
}
