/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SimpleSqmUpdateTranslation;
import org.hibernate.query.sqm.sql.SimpleSqmUpdateTranslator;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class StandardSqmUpdateTranslator
		extends BaseSqmToSqlAstConverter
		implements SimpleSqmUpdateTranslator {


	public StandardSqmUpdateTranslator(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings );
	}

	@Override
	public CteStatement translate(SqmCteStatement sqmCte) {
		return visitCteStatement( sqmCte );
	}

	@Override
	public SimpleSqmUpdateTranslation translate(SqmUpdateStatement sqmUpdate) {
		final UpdateStatement sqlUpdateAst = visitUpdateStatement( sqmUpdate );
		return new SimpleSqmUpdateTranslation(
				sqlUpdateAst,
				getJdbcParamsBySqmParam()
		);
	}

	@Override
	public UpdateStatement visitUpdateStatement(SqmUpdateStatement sqmStatement) {
		final String entityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = getCreationContext().getDomainModel().getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		getProcessingStateStack().push(
				new SqlAstProcessingStateImpl(
						null,
						this,
						getCurrentClauseStack()::getCurrent
				)
		);

		try {
			final NavigablePath rootPath = sqmStatement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					rootPath,
					null,
					false,
					LockMode.WRITE,
					getSqlAliasBaseGenerator(),
					getSqlExpressionResolver(),
					() -> predicate -> additionalRestrictions = predicate,
					getCreationContext()
			);

			if ( ! rootTableGroup.getTableReferenceJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM DELETE" );
			}

			getFromClauseIndex().registerTableGroup( rootPath, rootTableGroup );

			final List<Assignment> assignments = visitSetClause( sqmStatement.getSetClause() );

			Predicate suppliedPredicate = null;
			final SqmWhereClause whereClause = sqmStatement.getWhereClause();
			if ( whereClause != null && whereClause.getPredicate() != null ) {
				getCurrentClauseStack().push( Clause.WHERE );
				try {
					suppliedPredicate = (Predicate) whereClause.getPredicate().accept( this );
				}
				finally {
					getCurrentClauseStack().pop();
				}
			}

			return new UpdateStatement(
					rootTableGroup.getPrimaryTableReference(),
					assignments,
					SqlAstTreeHelper.combinePredicates( suppliedPredicate, additionalRestrictions )
			);
		}
		finally {
			getProcessingStateStack().pop();
		}
	}


	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return SQL_ALIAS_BASE_GENERATOR;
	}

	private static final SqlAliasBaseGenerator SQL_ALIAS_BASE_GENERATOR = new SqlAliasBaseGenerator() {
		private final SqlAliasBase sqlAliasBase = new SqlAliasBase() {
			@Override
			public String getAliasStem() {
				return null;
			}

			@Override
			public String generateNewAlias() {
				return null;
			}
		};

		@Override
		public SqlAliasBase createSqlAliasBase(String stem) {
			return sqlAliasBase;
		}
	};

	@Override
	public List<Assignment> visitSetClause(SqmSetClause setClause) {
		final List<Assignment> assignments = new ArrayList<>();

		final List<ColumnReference> targetColumnReferences = new ArrayList<>();

		for ( SqmAssignment sqmAssignment : setClause.getAssignments() ) {
			getProcessingStateStack().push(
					new SqlAstProcessingStateImpl(
							getProcessingStateStack().getCurrent(),
							this,
							getCurrentClauseStack()::getCurrent
					) {
						@Override
						public Expression resolveSqlExpression(
								String key,
								Function<SqlAstProcessingState, Expression> creator) {
							final Expression expression = getParentState().getSqlExpressionResolver().resolveSqlExpression( key, creator );
							assert expression instanceof ColumnReference;

							targetColumnReferences.add( (ColumnReference) expression );

							return expression;
						}
					}
			);

			final SqmPathInterpretation assignedPathInterpretation;
			try {
				assignedPathInterpretation = (SqmPathInterpretation) sqmAssignment.getTargetPath().accept( this );
			}
			finally {
				getProcessingStateStack().pop();
			}

			inferableTypeAccessStack.push( assignedPathInterpretation::getExpressionType );

			final List<ColumnReference> valueColumnReferences = new ArrayList<>();
			getProcessingStateStack().push(
					new SqlAstProcessingStateImpl(
							getProcessingStateStack().getCurrent(),
							this,
							getCurrentClauseStack()::getCurrent
					) {
						@Override
						public Expression resolveSqlExpression(
								String key,
								Function<SqlAstProcessingState, Expression> creator) {
							final Expression expression = getParentState().getSqlExpressionResolver().resolveSqlExpression( key, creator );
							assert expression instanceof ColumnReference;

							valueColumnReferences.add( (ColumnReference) expression );

							return expression;
						}
					}
			);

			try {

				if ( sqmAssignment.getValue() instanceof SqmParameter ) {
					final SqmParameter sqmParameter = (SqmParameter) sqmAssignment.getValue();
					final List<JdbcParameter> jdbcParametersForSqm = new ArrayList<>();

					// create one JdbcParameter for each column in the assigned path
					assignedPathInterpretation.getExpressionType().visitColumns(
							(columnExpression, containingTableExpression, jdbcMapping) -> {
								final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcMapping );
								jdbcParametersForSqm.add( jdbcParameter );
								assignments.add(
										new Assignment(
												new ColumnReference(
														// we do not want a qualifier (table alias) here
														(String) null,
														columnExpression,
														jdbcMapping,
														getCreationContext().getSessionFactory()
												),
												jdbcParameter
										)
								);
							}
					);

					getJdbcParamsBySqmParam().put( sqmParameter, jdbcParametersForSqm );
				}
				else {
					final MappingMetamodel domainModel = getCreationContext().getDomainModel();
					final TypeConfiguration typeConfiguration = domainModel.getTypeConfiguration();

					final Expression valueExpression = (Expression) sqmAssignment.getValue().accept( this );

					final int valueExprJdbcCount = valueExpression.getExpressionType().getJdbcTypeCount( typeConfiguration );
					final int assignedPathJdbcCount = assignedPathInterpretation.getExpressionType().getJdbcTypeCount( typeConfiguration );

					if ( valueExprJdbcCount != assignedPathJdbcCount ) {
						SqlTreeCreationLogger.LOGGER.debugf(
								"JDBC type count does not match in UPDATE assignment between the assigned-path and the assigned-value; " +
										"this will likely lead to problems executing the query"
						);
					}

					assert assignedPathJdbcCount == valueExprJdbcCount;

					if ( valueExpression instanceof ColumnReference ) {
						assert valueExprJdbcCount == 1;

						assignments.add( new Assignment( (ColumnReference) valueExpression, valueExpression ) );
					}
					else {
						throw new NotYetImplementedFor6Exception( "Support for composite valued assignments in an UPDATE query is not yet implemented" );
					}
				}
			}
			finally {
				getProcessingStateStack().pop();
				inferableTypeAccessStack.pop();
			}

		}

		return assignments;
	}
}
