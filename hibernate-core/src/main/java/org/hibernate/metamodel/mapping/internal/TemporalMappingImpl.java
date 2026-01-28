/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.time.Instant;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Temporalized;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static java.util.Collections.emptyList;
import static org.hibernate.query.sqm.ComparisonOperator.GREATER_THAN;
import static org.hibernate.query.sqm.ComparisonOperator.LESS_THAN_OR_EQUAL;

/**
 * Temporal mapping implementation.
 */
public class TemporalMappingImpl implements TemporalMapping {
	private final String tableName;
	private final SelectableMapping startingColumnMapping;
	private final SelectableMapping endingColumnMapping;
	private final JdbcMapping jdbcMapping;
	private final String currentTimestampFunctionName;
	private final Expression currentTimestampExpression;

	public TemporalMappingImpl(
			Temporalized bootMapping,
			String tableName,
			MappingModelCreationProcess creationProcess) {
		this.tableName = tableName;

		final var startingColumn = bootMapping.getTemporalStartingColumn();
		final var endingColumn = bootMapping.getTemporalEndingColumn();
		final var startingValue = (BasicValue) startingColumn.getValue();
		final var endingValue = (BasicValue) endingColumn.getValue();

		final var startingResolution = startingValue.resolve();
		final var endingResolution = endingValue.resolve();

		jdbcMapping = startingResolution.getJdbcMapping();
		if ( jdbcMapping != endingResolution.getJdbcMapping() ) {
			throw new IllegalStateException( "Temporal starting and ending columns must use the same JDBC mapping" );
		}

		final var creationContext = creationProcess.getCreationContext();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		final var dialect = creationContext.getDialect();
		final var sqmFunctionRegistry =
				creationContext.getSessionFactory().getQueryEngine()
						.getSqmFunctionRegistry();

		startingColumnMapping = SelectableMappingImpl.from(
				tableName,
				startingColumn,
				jdbcMapping,
				typeConfiguration,
				true,
				false,
				false,
				dialect,
				sqmFunctionRegistry,
				creationContext
		);
		endingColumnMapping = SelectableMappingImpl.from(
				tableName,
				endingColumn,
				jdbcMapping,
				typeConfiguration,
				true,
				true,
				false,
				dialect,
				sqmFunctionRegistry,
				creationContext
		);

		final boolean useServerTransactionTimestamps =
				creationContext.getSessionFactory().getSessionFactoryOptions()
						.isUseServerTransactionTimestampsEnabled();
		if ( useServerTransactionTimestamps ) {
			currentTimestampFunctionName = dialect.currentTimestamp();
			currentTimestampExpression =
					new SelfRenderingSqlFragmentExpression( currentTimestampFunctionName, jdbcMapping );
		}
		else {
			currentTimestampFunctionName = null;
			currentTimestampExpression = null;
		}
	}

	@Override
	public String getStartingColumnName() {
		return startingColumnMapping.getSelectionExpression();
	}

	@Override
	public String getEndingColumnName() {
		return endingColumnMapping.getSelectionExpression();
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public SelectableMapping getStartingColumnMapping() {
		return startingColumnMapping;
	}

	@Override
	public SelectableMapping getEndingColumnMapping() {
		return endingColumnMapping;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public Predicate createCurrentRestriction(TableReference tableReference) {
		return createCurrentRestriction( tableReference, null );
	}

	@Override
	public Predicate createCurrentRestriction(TableReference tableReference, SqlExpressionResolver expressionResolver) {
		final var endingColumn = resolveColumn( tableReference, expressionResolver, endingColumnMapping );
		return new NullnessPredicate( endingColumn, false, jdbcMapping );
	}

	@Override
	public Predicate createRestriction(TableReference tableReference, Instant instant) {
		return createRestriction( tableReference, null, instant );
	}

	@Override
	public Predicate createRestriction(
			TableReference tableReference,
			SqlExpressionResolver expressionResolver,
			Instant instant) {
		final var startingColumn = resolveColumn( tableReference, expressionResolver, startingColumnMapping );
		final var endingColumn = resolveColumn( tableReference, expressionResolver, endingColumnMapping );

		final var instantExpression =
				currentTimestampExpression == null || instant != null
						? new TemporalInstantParameter( jdbcMapping, instant )
						: currentTimestampExpression;

		final var startingPredicate = new ComparisonPredicate( startingColumn, LESS_THAN_OR_EQUAL, instantExpression );
		final var endingNullPredicate = new NullnessPredicate( endingColumn, false, jdbcMapping );
		final var endingAfterPredicate = new ComparisonPredicate( endingColumn, GREATER_THAN, instantExpression );

		final var endingPredicate = new Junction( Junction.Nature.DISJUNCTION );
		endingPredicate.add( endingNullPredicate );
		endingPredicate.add( endingAfterPredicate );

		final var predicate = new Junction( Junction.Nature.CONJUNCTION );
		predicate.add( startingPredicate );
		predicate.add( endingPredicate );

		return predicate;
	}

	@Override
	public ColumnValueBinding createStartingValueBinding(ColumnReference startingColumnReference) {
		return createTemporalValueBinding( startingColumnReference, startingColumnMapping );
	}

	@Override
	public ColumnValueBinding createEndingValueBinding(ColumnReference endingColumnReference) {
		return createTemporalValueBinding( endingColumnReference, endingColumnMapping );
	}

	private ColumnValueBinding createTemporalValueBinding(
			ColumnReference endingColumnReference, SelectableMapping columnMapping) {
		return new ColumnValueBinding( endingColumnReference,
				currentTimestampFunctionName != null
						? new ColumnWriteFragment( currentTimestampFunctionName, emptyList(), columnMapping )
						: new ColumnWriteFragment( "?",
								new ColumnValueParameter( endingColumnReference ),
								columnMapping ) );
	}

	@Override
	public ColumnValueBinding createNullEndingValueBinding(ColumnReference endingColumnReference) {
		return new ColumnValueBinding( endingColumnReference,
				new ColumnWriteFragment( null, emptyList(), endingColumnMapping ) );
	}

	private Expression resolveColumn(
			TableReference tableReference,
			SqlExpressionResolver expressionResolver,
			SelectableMapping selectableMapping) {
		return expressionResolver != null
				? expressionResolver.resolveSqlExpression( tableReference, selectableMapping )
				: new ColumnReference( tableReference, selectableMapping );
	}

	@Override
	public String toString() {
		return "TemporalMapping(" + tableName + "." + getStartingColumnName() + "," + getEndingColumnName() + ")";
	}

	private static class TemporalInstantParameter implements JdbcParameter, JdbcParameterBinder {
		private final JdbcMapping jdbcMapping;
		private final Instant value;

		private TemporalInstantParameter(JdbcMapping jdbcMapping, Instant value) {
			this.jdbcMapping = jdbcMapping;
			this.value = value;
		}

		@Override
		public JdbcParameterBinder getParameterBinder() {
			return this;
		}

		@Override
		public Integer getParameterId() {
			return null;
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParameterBindings,
				ExecutionContext executionContext) throws SQLException {
			jdbcMapping.getJdbcValueBinder().bind(
					statement,
					jdbcMapping.convertToRelationalValue( value ),
					startPosition,
					executionContext.getSession()
			);
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return jdbcMapping;
		}

		@Override
		public void accept(SqlAstWalker sqlTreeWalker) {
			sqlTreeWalker.visitParameter( this );
		}
	}
}
