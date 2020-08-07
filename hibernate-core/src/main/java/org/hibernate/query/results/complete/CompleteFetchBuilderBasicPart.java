/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FromClauseAccessImpl;
import org.hibernate.query.results.SqlAstCreationStateImpl;
import org.hibernate.query.results.SqlAstProcessingStateImpl;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.query.results.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class CompleteFetchBuilderBasicPart implements CompleteFetchBuilder {
	private final BasicValuedModelPart referencedModelPart;
	private final String columnName;

	public CompleteFetchBuilderBasicPart(BasicValuedModelPart referencedModelPart, String columnName) {
		this.referencedModelPart = referencedModelPart;
		this.columnName = columnName;
	}

	@Override
	public BasicValuedModelPart getReferencedPart() {
		return referencedModelPart;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl domainResultCreationStateImpl = impl( domainResultCreationState );
		final SqlAstCreationStateImpl sqlAstCreationState = domainResultCreationStateImpl.getSqlAstCreationState();
		final FromClauseAccessImpl fromClauseAccess = domainResultCreationStateImpl.getFromClauseAccess();
		final SqlAstProcessingStateImpl sqlAstProcessingState = sqlAstCreationState.getCurrentProcessingState();

		final String mappedTable = referencedModelPart.getContainingTableExpression();
		final String mappedColumn = referencedModelPart.getMappedColumnExpression();

		final TableGroup tableGroup = fromClauseAccess.getTableGroup( parent.getNavigablePath() );
		final TableReference tableReference = tableGroup.getTableReference( mappedTable );

		final String selectedAlias;
		final int jdbcPosition;

		if ( columnName == null ) {
			jdbcPosition = sqlAstProcessingState.getNumberOfProcessedSelections();
			selectedAlias = jdbcResultsMetadata.resolveColumnName( jdbcPosition );
		}
		else {
			jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnName );
			selectedAlias = columnName;
		}

		final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );

		// we just care about the registration here.  The ModelPart will find it later

		sqlAstProcessingState.resolveSqlExpression(
				createColumnReferenceKey( tableReference, mappedColumn ),
				processingState -> new SqlSelectionImpl( valuesArrayPosition, referencedModelPart )
		);
//		final Expression expression = sqlAstProcessingState.resolveSqlExpression(
//				createColumnReferenceKey( tableReference, mappedColumn ),
//				processingState -> new SqlSelectionImpl( valuesArrayPosition, referencedModelPart )
//		);
//
//		final SqlSelection sqlSelection = sqlAstProcessingState.resolveSqlSelection(
//				expression,
//				referencedModelPart.getJavaTypeDescriptor(),
//				sqlAstCreationState.getCreationContext().getSessionFactory().getTypeConfiguration()
//		);

		return referencedModelPart.generateFetch(
				parent,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				LockMode.READ,
				selectedAlias,
				domainResultCreationState
		);
	}
}
