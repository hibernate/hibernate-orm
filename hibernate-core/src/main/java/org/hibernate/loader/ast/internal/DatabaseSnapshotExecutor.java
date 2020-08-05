/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.RowTransformerDatabaseSnapshotImpl;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
class DatabaseSnapshotExecutor {
	private static final Logger log = Logger.getLogger( DatabaseSnapshotExecutor.class );

	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	private final JdbcSelect jdbcSelect;
	private final List<JdbcParameter> jdbcParameters;

	DatabaseSnapshotExecutor(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;

		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

		final LoaderSqlAstCreationState state = new LoaderSqlAstCreationState(
				rootQuerySpec,
				sqlAliasBaseManager,
				LockOptions.READ,
				sessionFactory
		);

		final NavigablePath rootPath = new NavigablePath( entityDescriptor.getEntityName() );

		final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				rootPath,
				null,
				true,
				LockMode.NONE,
				sqlAliasBaseManager,
				state.getSqlExpressionResolver(),
				() -> rootQuerySpec::applyPredicate,
				sessionFactory
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );

		jdbcParameters = new ArrayList<>(
				entityDescriptor.getIdentifierMapping().getJdbcTypeCount( sessionFactory.getTypeConfiguration() )
		);
		final List<DomainResult> domainResults = new ArrayList<>();

		entityDescriptor.getIdentifierMapping().visitColumns(
				(tab, col, isColFormula, jdbcMapping) -> {
					final TableReference tableReference = rootTableGroup.resolveTableReference( tab );

					final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcMapping );
					jdbcParameters.add( jdbcParameter );

					final ColumnReference columnReference = (ColumnReference) state.getSqlExpressionResolver()
							.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey( tableReference, col ),
									s -> new ColumnReference(
											tableReference,
											col,
											isColFormula,
											jdbcMapping,
											sessionFactory
									)
							);

					rootQuerySpec.applyPredicate(
							new ComparisonPredicate(
									columnReference,
									ComparisonOperator.EQUAL,
									jdbcParameter
							)
					);

					final SqlSelection sqlSelection = state.getSqlExpressionResolver().resolveSqlSelection(
							columnReference,
							jdbcMapping.getJavaTypeDescriptor(),
							sessionFactory.getTypeConfiguration()
					);

					//noinspection unchecked
					domainResults.add(
							new BasicResult(
									sqlSelection.getValuesArrayPosition(),
									null,
									jdbcMapping.getJavaTypeDescriptor()
							)
					);
				}
		);

		entityDescriptor.visitStateArrayContributors(
				contributorMapping -> {
					rootPath.append( contributorMapping.getAttributeName() );
					contributorMapping.visitColumns(
							(containingTableExpression, columnExpression, isColumnExpressionFormula, jdbcMapping) -> {
								final TableReference tableReference = rootTableGroup.resolveTableReference(
										containingTableExpression );

								final ColumnReference columnReference = (ColumnReference) state.getSqlExpressionResolver()
										.resolveSqlExpression(
												SqlExpressionResolver.createColumnReferenceKey(
														tableReference,
														columnExpression
												),
												s -> new ColumnReference(
														tableReference,
														columnExpression,
														isColumnExpressionFormula,
														jdbcMapping,
														sessionFactory
												)
										);

								final SqlSelection sqlSelection = state.getSqlExpressionResolver()
										.resolveSqlSelection(
												columnReference,
												jdbcMapping.getJavaTypeDescriptor(),
												sessionFactory.getTypeConfiguration()
										);

								//noinspection unchecked
								domainResults.add(
										new BasicResult(
												sqlSelection.getValuesArrayPosition(),
												null,
												jdbcMapping.getJavaTypeDescriptor()
										)
								);
							}
					);
				}
		);

		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec, domainResults );

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( selectStatement );
	}

	Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Getting current persistent state for `%s#%s`", entityDescriptor.getEntityName(), id );
		}

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				entityDescriptor.getIdentifierMapping().getJdbcTypeCount( sessionFactory.getTypeConfiguration() )
		);

		final Iterator<JdbcParameter> paramItr = jdbcParameters.iterator();

		entityDescriptor.getIdentifierMapping().visitJdbcValues(
				id,
				Clause.WHERE,
				(value, type) -> {
					assert paramItr.hasNext();
					final JdbcParameter parameter = paramItr.next();
					jdbcParameterBindings.addBinding(
							parameter,
							new JdbcParameterBindingImpl( type, value )
					);
				},
				session
		);
		assert !paramItr.hasNext();

		final List list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				jdbcParameterBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						return null;
					}
				},
				RowTransformerDatabaseSnapshotImpl.instance(),
				true
		);

		if ( list.isEmpty() ) {
			return null;
		}

		final int size = list.size();
		assert size <= 1;

		if ( size == 0 ) {
			return null;
		}
		else {
			return (Object[]) list.get( 0 );
		}
	}

}
