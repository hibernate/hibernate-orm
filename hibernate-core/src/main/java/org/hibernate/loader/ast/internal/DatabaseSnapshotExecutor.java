/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.sql.FromClauseIndex;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
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
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.IntegerType;

import org.jboss.logging.Logger;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
class DatabaseSnapshotExecutor {
	private static final Logger log = Logger.getLogger( DatabaseSnapshotExecutor.class );

	private final EntityMappingType entityDescriptor;

	private final JdbcSelect jdbcSelect;
	private final List<JdbcParameter> jdbcParameters;

	DatabaseSnapshotExecutor(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.jdbcParameters = new ArrayList<>(
				entityDescriptor.getIdentifierMapping().getJdbcTypeCount()
		);

		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

		final LoaderSqlAstCreationState state = new LoaderSqlAstCreationState(
				rootQuerySpec,
				sqlAliasBaseManager,
				new FromClauseIndex( null ),
				LockOptions.READ,
				(fetchParent, ast, creationState) -> Collections.emptyList(),
				true,
				sessionFactory
		);

		final NavigablePath rootPath = new NavigablePath( entityDescriptor.getEntityName() );

		final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				rootPath,
				null,
				() -> rootQuerySpec::applyPredicate,
				state,
				sessionFactory
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		state.getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

		// We produce the same state array as if we were creating an entity snapshot
		final List<DomainResult<?>> domainResults = new ArrayList<>();

		final SqlExpressionResolver sqlExpressionResolver = state.getSqlExpressionResolver();

		// We just need a literal to have a result set
		domainResults.add(
				new QueryLiteral<>( null, IntegerType.INSTANCE ).createDomainResult( null, state )
		);
		final NavigablePath idNavigablePath = rootPath.append( entityDescriptor.getIdentifierMapping().getNavigableRole().getNavigableName() );
		entityDescriptor.getIdentifierMapping().forEachSelectable(
				(columnIndex, selection) -> {
					final TableReference tableReference = rootTableGroup.resolveTableReference(
							idNavigablePath,
							selection.getContainingTableExpression()
					);

					final JdbcParameter jdbcParameter = new JdbcParameterImpl( selection.getJdbcMapping() );
					jdbcParameters.add( jdbcParameter );

					final ColumnReference columnReference = (ColumnReference) sqlExpressionResolver
							.resolveSqlExpression(
									createColumnReferenceKey( tableReference, selection.getSelectionExpression() ),
									s -> new ColumnReference(
											tableReference,
											selection,
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
				}
		);

		entityDescriptor.visitStateArrayContributors(
				contributorMapping -> {
					final NavigablePath navigablePath = rootPath.append( contributorMapping.getAttributeName() );
					if ( contributorMapping instanceof SingularAttributeMapping ) {
						if ( contributorMapping instanceof EntityAssociationMapping ) {
							domainResults.add(
									( (EntityAssociationMapping) contributorMapping ).createDelayedDomainResult(
											navigablePath,
											rootTableGroup,
											null,
											state
									)
							);
						}
						else {
							domainResults.add(
									contributorMapping.createDomainResult(
											navigablePath,
											rootTableGroup,
											null,
											state
									)
							);
						}
					}
					else {
						// TODO: Instead use a delayed collection result? Or will we remove this when redesigning this
						//noinspection unchecked
						domainResults.add(
								new BasicResult(
										0,
										null,
										contributorMapping.getJavaTypeDescriptor()
								)
						);
					}
				}
		);
		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec, domainResults );

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		this.jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, selectStatement )
				.translate( null, QueryOptions.NONE );
	}

	Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Getting current persistent state for `%s#%s`", entityDescriptor.getEntityName(), id );
		}

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				entityDescriptor.getIdentifierMapping().getJdbcTypeCount()
		);

		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				id,
				Clause.WHERE,
				entityDescriptor.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final List<?> list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
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
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		final int size = list.size();
		assert size <= 1;

		if ( size == 0 ) {
			return null;
		}
		else {
			final Object[] entitySnapshot = (Object[]) list.get( 0 );
			// The result of this method is treated like the entity state array which doesn't include the id
			// So we must exclude it from the array
			if ( entitySnapshot.length == 1 ) {
				return ArrayHelper.EMPTY_OBJECT_ARRAY;
			}
			else {
				final Object[] state = new Object[entitySnapshot.length - 1];
				System.arraycopy( entitySnapshot, 1, state, 0, state.length );
				return state;
			}
		}
	}

}
