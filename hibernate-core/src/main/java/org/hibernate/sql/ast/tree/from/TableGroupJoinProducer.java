/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.function.Consumer;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

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
	 * Create a TableGroupJoin.
	 *
	 * @param navigablePath The NavigablePath to the join
	 * @param lhs The join's (L)eft-(H)and (S)ide
	 * @param explicitSqlAliasBase A specific SqlAliasBase to use.  May be {@code null} indicating one should be created using the {@linkplain SqlAliasBaseGenerator} from {@code creationState}
	 * @param sqlAstJoinType An explicit join-type. May be null to signal that the join is for an implicit path.
	 * @param addsPredicate Indicates there are explicit, additional predicates (from an SQM tree ON/WITH clause)
	 */
	TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState);

	/**
	 * Create the "join", but return a TableGroup.  Intended for creating sub-query
	 * correlations.  E.g., given
	 * <pre>
	 *     from SomeEntity e
	 *     where exists (
	 *         select 1
	 *         from AnotherEntity a
	 *         where e.association.attr = 1
	 *     )
	 * </pre>
	 * We call this for the `e.association` path.
	 *
	 * @param navigablePath The NavigablePath to the join
	 * @param lhs The join's (L)eft-(H)and (S)ide
	 * @param explicitSqlAliasBase A specific SqlAliasBase to use.  May be {@code null} indicating one should be created using the {@linkplain SqlAliasBaseGenerator} from {@code creationState}
	 * @param sqlAstJoinType An explicit join-type. May be null to signal that the join is for an implicit path.
	 * @param predicateConsumer Consumer for additional predicates from the producer's mapping.
	 */
	TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState);

	default SqlAstJoinType determineSqlJoinType(TableGroup lhs, @Nullable SqlAstJoinType requestedJoinType, boolean fetched) {
		if ( requestedJoinType != null ) {
			return requestedJoinType;
		}

		if ( fetched ) {
			return getDefaultSqlAstJoinType( lhs );
		}

		return SqlAstJoinType.INNER;
	}
}
