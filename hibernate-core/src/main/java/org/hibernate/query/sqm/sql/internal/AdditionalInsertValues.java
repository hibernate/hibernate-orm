/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
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
import org.hibernate.sql.exec.spi.ExecutionContext;
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

	public void applyValues(Values values) {
		final var expressions = values.getExpressions();
		if ( versionExpression != null ) {
			expressions.add( versionExpression );
		}
		if ( discriminatorExpression != null ) {
			expressions.add( discriminatorExpression );
		}
		if ( identifierGenerator != null && !identifierGenerator.generatedOnExecution() ) {
			if ( identifierGeneratorParameter == null ) {
				identifierGeneratorParameter =
						new IdGeneratorParameter( identifierMapping, (BeforeExecutionGenerator) identifierGenerator );
			}
			expressions.add( identifierGeneratorParameter );
		}
	}

	/// Apply implicit selections and report whether a separately generated row
	/// number must stand in for the identifier.
	public boolean applySelections(QuerySpec querySpec, SessionFactoryImplementor sessionFactory) {
		final var selectClause = querySpec.getSelectClause();
		if ( versionExpression != null ) {
			if ( versionSelection == null ) {
				versionSelection = new SqlSelectionImpl( versionExpression );
			}
			selectClause.addSqlSelection( versionSelection );
		}
		if ( discriminatorExpression != null ) {
			if ( discriminatorSelection == null ) {
				discriminatorSelection = new SqlSelectionImpl( discriminatorExpression );
			}
			selectClause.addSqlSelection( discriminatorSelection );
		}
		if ( identifierGenerator != null ) {
			if ( identifierSelection == null ) {
				if ( !( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator bulkGenerator ) ) {
					throw new SemanticException(
							"SQM INSERT-SELECT without bulk insertion capable identifier generator: "
									+ identifierGenerator
					);
				}
				if ( identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
					final var optimizer = optimizableGenerator.getOptimizer();
					if ( optimizer != null && optimizer.getIncrementSize() > 1
							|| !bulkGenerator.supportsBulkInsertionIdentifierGeneration() ) {
						if ( !sessionFactory.getJdbcServices().getDialect().getWindowFunctionSupport()
								.supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS ) ) {
							return false;
						}
						identifierSelection =
								new SqlSelectionImpl( createRowNumberingExpression( querySpec, sessionFactory ) );
						selectClause.addSqlSelection( identifierSelection );
						return true;
					}
				}
				final var fragment = bulkGenerator.determineBulkInsertionIdentifierGenerationSelectFragment(
						sessionFactory.getSqlStringGenerationContext()
				);
				identifierSelection = new SqlSelectionImpl( new SelfRenderingSqlFragmentExpression( fragment ) );
			}
			selectClause.addSqlSelection( identifierSelection );
		}
		return requiresRowNumberIntermediate();
	}

	public boolean requiresRowNumberIntermediate() {
		return identifierSelection != null
				&& !( identifierSelection.getExpression() instanceof SelfRenderingSqlFragmentExpression );
	}

	private static class IdGeneratorParameter extends AbstractJdbcParameter {
		private final BeforeExecutionGenerator generator;

		private IdGeneratorParameter(
				BasicEntityIdentifierMapping identifierMapping,
				BeforeExecutionGenerator generator) {
			super( identifierMapping.getJdbcMapping() );
			this.generator = generator;
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParamBindings,
				ExecutionContext executionContext) throws SQLException {
			getJdbcMapping().getJdbcValueBinder().bind(
					statement,
					generator.generate( executionContext.getSession(), null, null, INSERT ),
					startPosition,
					executionContext.getSession()
			);
		}
	}
}
