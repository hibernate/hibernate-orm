/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.function.Consumer;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public interface TableGroupJoinProducer extends TableGroupProducer {

	SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup);

	/**
	 * Returns whether the given predicate is a simple join predicate for this attribute.
	 * This is useful to understand if a predicate has additional conjunctions other than the FK related predicate.
	 */
	boolean isSimpleJoinPredicate(Predicate predicate);

	/**
	 * Create a TableGroupJoin as defined for this producer
	 *
	 * The sqlAstJoinType may be null to signal that the join is for an implicit path.
	 * When addsPredicate is <code>true</code>, the SQM join for the attribute contains an explicit <code>ON</code> clause,
	 * and is <code>false</code> otherwise.
	 */
	default TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		return createTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				sqlAstJoinType,
				fetched,
				addsPredicate,
				creationState.getSqlAliasBaseGenerator(),
				creationState.getSqlExpressionResolver(),
				creationState.getFromClauseAccess(),
				creationState.getCreationContext()
		);
	}

	/**
	 * Create a TableGroupJoin as defined for this producer
	 *
	 * The sqlAstJoinType may be null to signal that the join is for an implicit path.
	 * When addsPredicate is <code>true</code>, the SQM join for the attribute contains an explicit <code>ON</code> clause,
	 * and is <code>false</code> otherwise.
	 */
	TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext);

	/**
	 * Create a TableGroupJoin as defined for this producer, but as root TableGroup.
	 * The main purpose of this is for correlating an association in a subquery
	 * i.e. `...  alias where exists (select 1 from SomeEntity e where alias.association.attr = 1)`.
	 *
	 * The sqlAstJoinType may be null to signal that the join is for an implicit path.
	 */
	default TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				sqlAstJoinType,
				fetched,
				predicateConsumer,
				creationState.getSqlAliasBaseGenerator(),
				creationState.getSqlExpressionResolver(),
				creationState.getFromClauseAccess(),
				creationState.getCreationContext()
		);
	}

	/**
	 * Create a TableGroupJoin as defined for this producer, but as root TableGroup.
	 * The main purpose of this is for correlating an association in a subquery
	 * i.e. `...  alias where exists (select 1 from SomeEntity e where alias.association.attr = 1)`.
	 *
	 * The sqlAstJoinType may be null to signal that the join is for an implicit path.
	 */
	TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext);
}
