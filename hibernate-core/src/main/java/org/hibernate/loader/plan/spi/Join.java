/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
