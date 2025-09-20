/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.sql.FromClauseIndex;
import org.hibernate.spi.NavigablePath;
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
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

import static org.hibernate.loader.LoaderLogging.LOADER_LOGGER;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * @author Steve Ebersole
 */
class DatabaseSnapshotExecutor {

	private final EntityMappingType entityDescriptor;

	private final JdbcOperationQuerySelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;

	DatabaseSnapshotExecutor(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder(
				entityDescriptor.getIdentifierMapping().getJdbcTypeCount() );
		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

		final LoaderSqlAstCreationState state = new LoaderSqlAstCreationState(
				rootQuerySpec,
				sqlAliasBaseManager,
				new FromClauseIndex( null ),
				new LockOptions(),
				(fetchParent, creationState) -> ImmutableFetchList.EMPTY,
				true,
				new LoadQueryInfluencers( sessionFactory ),
				sessionFactory.getSqlTranslationEngine()
		);

		final NavigablePath rootPath = new NavigablePath( entityDescriptor.getEntityName() );

		final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				true,
				rootPath,
				null,
				null,
				() -> rootQuerySpec::applyPredicate,
				state
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		state.getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

		// We produce the same state array as if we were creating an entity snapshot
		final List<DomainResult<?>> domainResults = new ArrayList<>();

		final SqlExpressionResolver sqlExpressionResolver = state.getSqlExpressionResolver();

		// We just need a literal to have a result set
		final BasicType<Integer> resolved = sessionFactory.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		final QueryLiteral<Integer> queryLiteral = new QueryLiteral<>( null, resolved );
		domainResults.add( queryLiteral.createDomainResult( null, state ) );
		final NavigablePath idNavigablePath = rootPath.append( entityDescriptor.getIdentifierMapping().getNavigableRole().getNavigableName() );
		entityDescriptor.getIdentifierMapping().forEachSelectable(
				(columnIndex, selection) -> {
					final TableReference tableReference = rootTableGroup.resolveTableReference(
							idNavigablePath,
							selection.getContainingTableExpression()
					);

					final JdbcParameter jdbcParameter = new JdbcParameterImpl( selection.getJdbcMapping() );
					jdbcParametersBuilder.add( jdbcParameter );

					final ColumnReference columnReference = (ColumnReference) sqlExpressionResolver
							.resolveSqlExpression( tableReference, selection );

					rootQuerySpec.applyPredicate(
							new ComparisonPredicate(
									columnReference,
									ComparisonOperator.EQUAL,
									jdbcParameter
							)
					);
				}
		);
		this.jdbcParameters = jdbcParametersBuilder.build();


		entityDescriptor.forEachAttributeMapping(
				attributeMapping -> {
					final NavigablePath navigablePath = rootPath.append( attributeMapping.getAttributeName() );
					final DomainResult<Object> snapshotDomainResult = attributeMapping.createSnapshotDomainResult(
							navigablePath,
							rootTableGroup,
							null,
							state
					);
					if ( snapshotDomainResult != null ) {
						domainResults.add( snapshotDomainResult );
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
		if ( LOADER_LOGGER.isTraceEnabled() ) {
			LOADER_LOGGER.trace( "Retrieving snapshot of current persistent state for "
					+ infoString( entityDescriptor, id ) );
		}

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				entityDescriptor.getIdentifierMapping().getJdbcTypeCount()
		);

		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				id,
				entityDescriptor.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final List<?> list = session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new BaseExecutionContext( session ),
				RowTransformerArrayImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
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
