/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.sql.ast.JoinType;

/**
 * Used in constructing {@link TableGroup} references to collect the individual table
 * references
 *
 * @author Steve Ebersole
 */
public interface TableReferenceCollector {
	void applyPrimaryReference(TableReference tableReference);

	/**
	 * Collect a table reference as part of the TableGroup.
	 *
	 * @param tableReference The TableReference.
	 * @param joinType The type of join indicated by the mapping of the table, if it is to be joined
	 * @param predicateProducer Function for creating the join predicate, if it is to be joined.  The first
	 * 		argument passed to the function is the LHS reference.  The second is the same as `tableReference`.
	 * 		The result is a SQL AST Predicate to use as the join-predicate
	 */
	void applySecondaryTableReferences(
			TableReference tableReference,
			JoinType joinType,
			TableReferenceJoinPredicateProducer predicateProducer);

	/**
	 * Directly add a TableReferenceJoin
	 */
	void addTableReferenceJoin(TableReferenceJoin join);
}
