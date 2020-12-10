/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.query.results.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderEmbeddable implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final EmbeddableValuedFetchable fetchable;

	public ImplicitFetchBuilderEmbeddable(
			NavigablePath fetchPath,
			EmbeddableValuedFetchable fetchable) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState creationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().resolveTableGroup(
				fetchPath,
				navigablePath -> {
					final TableGroup parentTableGroup = creationStateImpl
							.getFromClauseAccess()
							.getTableGroup( parent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = fetchable.createTableGroupJoin(
							fetchPath,
							parentTableGroup,
							null,
							SqlAstJoinType.INNER,
							LockMode.READ,
							creationStateImpl
					);
					return tableGroupJoin.getJoinedGroup();
				}
		);

		fetchable.forEachSelection(
				(columnIndex, selection) -> {
					final TableReference tableReference = tableGroup.getTableReference( selection.getContainingTableExpression() );

					final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( selection.getSelectionExpression() );
					final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );

					final Expression expression = creationStateImpl.resolveSqlExpression(
							createColumnReferenceKey( tableReference, selection.getSelectionExpression() ),
							processingState -> new SqlSelectionImpl(
									valuesArrayPosition,
									selection.getJdbcMapping()
							)
					);

					creationStateImpl.resolveSqlSelection(
							expression,
							selection.getJdbcMapping().getJavaTypeDescriptor(),
							creationStateImpl.getSessionFactory().getTypeConfiguration()
					);
				}
		);

		return fetchable.generateFetch(
				parent,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				LockMode.READ,
				null,
				creationState
		);
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderEmbeddable(" + fetchPath + ")";
	}
}
