package org.hibernate.query.sqm.sql.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.GenerationRequests;
import org.hibernate.generator.Generator;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.query.SemanticException;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.spi.query.insert.Values;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper.createRowNumberingExpression;

/// Internal state used while applying implicit values to an SQM insert.
///
/// @author Steve Ebersole
@Internal
public final class AdditionalInsertValues {
	private final Expression versionExpression;
	private final Expression discriminatorExpression;
	private final Generator identifierGenerator;
	private final BasicEntityIdentifierMapping identifierMapping;
	private Expression identifierGeneratorParameter;
	private SqlSelection versionSelection;
	private SqlSelection discriminatorSelection;
	private SqlSelection identifierSelection;

	public AdditionalInsertValues(
			Expression versionExpression,
			Expression discriminatorExpression,
			Generator identifierGenerator,
			BasicEntityIdentifierMapping identifierMapping) {
		this.versionExpression = versionExpression;
		this.discriminatorExpression = discriminatorExpression;
		this.identifierGenerator = identifierGenerator;
		this.identifierMapping = identifierMapping;
	}

	public void applyValues(Values values, int valuesListSize) {
		final List<Expression> expressions = values.getExpressions();
		if ( versionExpression != null ) {
			expressions.add( versionExpression );
		}
		if ( discriminatorExpression != null ) {
			expressions.add( discriminatorExpression );
		}
		if ( identifierGenerator != null && !identifierGenerator.generatedOnExecution() ) {
			if ( identifierGeneratorParameter == null ) {
				final var generator = (BeforeExecutionGenerator) identifierGenerator;
				identifierGeneratorParameter =
						new IdGeneratorParameter( identifierMapping, generator, valuesListSize );
			}
			expressions.add( identifierGeneratorParameter );
		}
	}

	/**
	 * Returns true if the identifier can't be applied directly and needs to be generated separately.
	 * As a replacement for the identifier, the special row_number column should be filled.
	 */
	public boolean applySelections(QuerySpec querySpec, SessionFactoryImplementor sessionFactory) {
		final var selectClause = querySpec.getSelectClause();
		if ( versionExpression != null ) {
			if ( versionSelection == null ) {
				// The position is irrelevant as this is only needed for insert
				versionSelection = new SqlSelectionImpl( versionExpression );
			}
			selectClause.addSqlSelection( versionSelection );
		}
		if ( discriminatorExpression != null ) {
			if ( discriminatorSelection == null ) {
				// The position is irrelevant as this is only needed for insert
				discriminatorSelection = new SqlSelectionImpl( discriminatorExpression );
			}
			selectClause.addSqlSelection( discriminatorSelection );
		}
		if ( identifierGenerator != null ) {
			if ( identifierSelection == null ) {
				if ( !(identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator bulkInsertionCapableGenerator) ) {
					throw new SemanticException(
							"SQM INSERT-SELECT without bulk insertion capable identifier generator: " + identifierGenerator );
				}
				if ( identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
					final var optimizer = optimizableGenerator.getOptimizer();
					if ( optimizer != null && optimizer.getIncrementSize() > 1
						|| !bulkInsertionCapableGenerator.supportsBulkInsertionIdentifierGeneration() ) {
						// This is a special case where we have a sequence with an optimizer
						// or a table based identifier generator
						if ( !sessionFactory.getJdbcServices().getDialect().getWindowFunctionSupport().supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS ) ) {
							return false;
						}
						else {
							identifierSelection =
									new SqlSelectionImpl( createRowNumberingExpression( querySpec, sessionFactory ) );
							selectClause.addSqlSelection( identifierSelection );
							return true;
						}
					}
				}
				final String fragment =
						bulkInsertionCapableGenerator.determineBulkInsertionIdentifierGenerationSelectFragment(
								sessionFactory.getSqlStringGenerationContext()
						);
				// The position is irrelevant as this is only needed for insert
				identifierSelection = new SqlSelectionImpl( new SelfRenderingSqlFragmentExpression( fragment ) );
			}
			selectClause.addSqlSelection( identifierSelection );
		}
		return requiresRowNumberIntermediate();
	}

	public boolean requiresRowNumberIntermediate() {
		return identifierSelection != null
			&& !(identifierSelection.getExpression() instanceof SelfRenderingSqlFragmentExpression);
	}

	private static class IdGeneratorParameter extends AbstractJdbcParameter {

		private final BeforeExecutionGenerator generator;
		private final int neededIdCount;

		public IdGeneratorParameter(BasicEntityIdentifierMapping identifierMapping, BeforeExecutionGenerator generator, int neededIdCount) {
			super( identifierMapping.getJdbcMapping() );
			this.generator = generator;
			this.neededIdCount = neededIdCount;
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParamBindings,
				ExecutionContext executionContext) throws SQLException {
			final SharedSessionContractImplementor session = executionContext.getSession();
			if ( generator.supportsBatchGeneration() ) {
				JdbcParameterBinding binding = jdbcParamBindings.getBinding( this );
				if ( binding == null ) {
					final Object[] ids =
							generator.generateBatch( session, GenerationRequests.of( neededIdCount ), INSERT );
					binding = new JdbcParameterBindingImpl( null, new IdValues( ids ) );
					jdbcParamBindings.addBinding( this, binding );
				}
				final IdValues idValues = (IdValues) binding.getBindValue();
				getJdbcMapping().getJdbcValueBinder().bind(
						statement,
						idValues.nextValue(),
						startPosition,
						session
				);
				if ( !idValues.hasNext() ) {
					// Remove the binding at the end to be safe
					jdbcParamBindings.addBinding( this, null );
				}
			}
			else {
				getJdbcMapping().getJdbcValueBinder().bind(
						statement,
						generator.generate( session, null, null, INSERT ),
						startPosition,
						session
				);
			}
		}

		private static final class IdValues {
			private final Object[] values;
			private int nextIndex;

			private IdValues(Object[] values) {
				this.values = values;
			}

			public Object nextValue() {
				return values[nextIndex++];
			}

			public boolean hasNext() {
				return nextIndex < values.length;
			}
		}
	}
}
