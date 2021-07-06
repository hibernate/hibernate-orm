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
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * Base support for NaturalIdLoader implementations
 */
public abstract class AbstractNaturalIdLoader<T> implements NaturalIdLoader<T> {

	// todo (6.0) : account for nullable attributes that are part of the natural-id (is-null-or-equals)
	// todo (6.0) : cache the SQL AST and JdbcParameter list

	private final NaturalIdMapping naturalIdMapping;
	private final EntityMappingType entityDescriptor;

	public AbstractNaturalIdLoader(
			NaturalIdMapping naturalIdMapping,
			EntityMappingType entityDescriptor) {
		this.naturalIdMapping = naturalIdMapping;
		this.entityDescriptor = entityDescriptor;
	}

	protected EntityMappingType entityDescriptor() {
		return entityDescriptor;
	}

	protected NaturalIdMapping naturalIdMapping() {
		return naturalIdMapping;
	}

	@Override
	public Loadable getLoadable() {
		return entityDescriptor();
	}

	@Override
	public T load(Object naturalIdValue, NaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		return selectByNaturalId(
				naturalIdMapping().normalizeInput( naturalIdValue, session ),
				options,
				(tableGroup, creationState) -> entityDescriptor.createDomainResult(
						new NavigablePath( entityDescriptor().getRootPathName() ),
						tableGroup,
						null,
						creationState
				),
				(fetchParent, querySpec, creationState) -> {
					final List<Fetch> fetches = new ArrayList<>( naturalIdMapping.getNaturalIdAttributes().size() );
					fetchParent.getReferencedMappingContainer().visitFetchables(
							fetchable -> {
								final NavigablePath navigablePath = fetchParent.resolveNavigablePath( fetchable );
								final Fetch fetch = fetchParent.generateFetchableFetch(
										fetchable,
										navigablePath,
										fetchable.getMappedFetchOptions().getTiming(),
										true,
										null,
										creationState
								);
								fetches.add( fetch );
							},
							entityDescriptor
					);
					return fetches;
				},
				(statsEnabled) -> {
//					entityDescriptor().getPreLoadListener().startingLoad( entityDescriptor, naturalIdValue, KeyType.NATURAL_ID, LoadSource.DATABASE );
					return statsEnabled ? System.nanoTime() : -1;
				},
				(result,startToken) -> {
//					entityDescriptor().getPostLoadListener().completedLoad( result, entityDescriptor(), naturalIdValue, KeyType.NATURAL_ID, LoadSource.DATABASE );
					if ( startToken > 0 ) {
						session.getFactory().getStatistics().naturalIdQueryExecuted(
								entityDescriptor().getEntityPersister().getRootEntityName(),
								System.nanoTime() - startToken
						);
//						// todo (6.0) : need a "load-by-natural-id" stat
//						//		e.g.,
//						// final Object identifier = entityDescriptor().getIdentifierMapping().getIdentifier( result, session );
//						// session.getFactory().getStatistics().entityLoadedByNaturalId( entityDescriptor(), identifier );
					}
				},
				session
		);
	}

	/**
	 * Perform a select, restricted by natural-id, based on `domainResultProducer` and `fetchProcessor`
	 */
	protected <L> L selectByNaturalId(
			Object bindValue,
			NaturalIdLoadOptions options,
			BiFunction<TableGroup,LoaderSqlAstCreationState, DomainResult<?>> domainResultProducer,
			LoaderSqlAstCreationState.FetchProcessor fetchProcessor,
			Function<Boolean,Long> statementStartHandler,
			BiConsumer<Object,Long> statementCompletionHandler,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final LockOptions lockOptions;
		if ( options.getLockOptions() != null ) {
			lockOptions = options.getLockOptions();
		}
		else {
			lockOptions = LockOptions.NONE;
		}

		final NavigablePath entityPath = new NavigablePath( entityDescriptor.getRootPathName() );
		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				fetchProcessor,
				true,
				sessionFactory
		);

		final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				true,
				entityPath,
				null,
				() -> rootQuerySpec::applyPredicate,
				sqlAstCreationState,
				sessionFactory
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		final DomainResult<?> domainResult = domainResultProducer.apply( rootTableGroup, sqlAstCreationState );

		final SelectStatement sqlSelect = new SelectStatement( rootQuerySpec, Collections.singletonList( domainResult ) );

		final List<JdbcParameter> jdbcParameters = new ArrayList<>( naturalIdMapping.getJdbcTypeCount() );
		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );

		applyNaturalIdRestriction(
				bindValue,
				rootTableGroup,
				rootQuerySpec::applyPredicate,
				(jdbcParameter, jdbcParameterBinding) -> {
					jdbcParameters.add( jdbcParameter );
					jdbcParamBindings.addBinding( jdbcParameter, jdbcParameterBinding );
				},
				sqlAstCreationState,
				session
		);

		final QueryOptions queryOptions = new SimpleQueryOptions( lockOptions, false );
		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlSelect )
				.translate( jdbcParamBindings, queryOptions );

		final StatisticsImplementor statistics = sessionFactory.getStatistics();
		final Long startToken = statementStartHandler.apply( statistics.isStatisticsEnabled() );

		//noinspection unchecked
		final List<L> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return queryOptions;
					}

					@Override
					public String getQueryIdentifier(String sql) {
						return sql;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						throw new UnsupportedOperationException( "Follow-on locking not supported yet" );
					}

				},
				row -> (L) row[0],
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Loading by natural-id returned more that one row : %s",
							entityDescriptor.getEntityName()
					)
			);
		}

		final L result;
		if ( results.isEmpty() ) {
			result = null;
		}
		else {
			result = results.get( 0 );
		}

		statementCompletionHandler.accept( result, startToken );

		return result;
	}

	/**
	 * Apply restriction necessary to match the given natural-id value.  Should
	 * apply any predicates to `predicateConsumer` as well and any parameter / binding
	 * pairs
	 */
	protected abstract void applyNaturalIdRestriction(
			Object bindValue,
			TableGroup rootTableGroup,
			Consumer<Predicate> predicateConsumer,
			BiConsumer<JdbcParameter, JdbcParameterBinding> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState,
			SharedSessionContractImplementor session);

	/**
	 * Helper to resolve ColumnReferences
	 */
	protected Expression resolveColumnReference(
			TableGroup rootTableGroup,
			SelectableMapping selectableMapping,
			SqlExpressionResolver sqlExpressionResolver,
			SessionFactoryImplementor sessionFactory) {
		final TableReference tableReference = rootTableGroup.getTableReference( rootTableGroup.getNavigablePath(), selectableMapping.getContainingTableExpression() );
		if ( tableReference == null ) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Unable to locate TableReference for `%s` : %s",
							selectableMapping.getContainingTableExpression(),
							rootTableGroup
					)
			);
		}
		return sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( tableReference, selectableMapping.getSelectionExpression() ),
				(processingState) -> new ColumnReference(
						tableReference,
						selectableMapping.getSelectionExpression(),
						selectableMapping.isFormula(),
						selectableMapping.getCustomReadExpression(),
						selectableMapping.getCustomWriteExpression(),
						selectableMapping.getJdbcMapping(),
						sessionFactory
				)
		);
	}

	@Override
	public Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
		return selectByNaturalId(
				naturalIdMapping().normalizeInput( naturalIdValue, session ),
				NaturalIdLoadOptions.NONE,
				(tableGroup, creationState) -> entityDescriptor.getIdentifierMapping().createDomainResult(
						tableGroup.getNavigablePath().append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
						tableGroup,
						null,
						creationState
				),
				(fetchParent, querySpec, creationState) -> {
					final List<Fetch> fetches = new ArrayList<>();

					fetchParent.getReferencedMappingContainer().visitFetchables(
							(fetchable) -> {
								final NavigablePath navigablePath = fetchParent.resolveNavigablePath( fetchable );
								final Fetch fetch = fetchParent.generateFetchableFetch(
										fetchable,
										navigablePath,
										fetchable.getMappedFetchOptions().getTiming(),
										true,
										null,
										creationState
								);
								fetches.add( fetch );
							},
							null
					);

					return fetches;
				},
				(statsEnabled) -> {
//					entityDescriptor().getPreLoadListener().startingLoad( entityDescriptor, naturalIdValue, KeyType.NATURAL_ID, LoadSource.DATABASE );
					return statsEnabled ? System.nanoTime() : -1L;
				},
				(result, startToken) -> {
//					entityDescriptor().getPostLoadListener().completedLoad( result, entityDescriptor(), naturalIdValue, KeyType.NATURAL_ID, LoadSource.DATABASE );
					if ( startToken > 0 ) {
						session.getFactory().getStatistics().naturalIdQueryExecuted(
								entityDescriptor().getEntityPersister().getRootEntityName(),
								System.nanoTime() - startToken
						);
//						// todo (6.0) : need a "load-by-natural-id" stat
//						//		e.g.,
//						// final Object identifier = entityDescriptor().getIdentifierMapping().getIdentifier( result, session );
//						// session.getFactory().getStatistics().entityLoadedByNaturalId( entityDescriptor(), identifier );
					}
				},
				session
		);
	}

	@Override
	public Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor(),
				Collections.singletonList( naturalIdMapping() ),
				entityDescriptor().getIdentifierMapping(),
				null,
				1,
				session.getLoadQueryInfluencers(),
				LockOptions.NONE,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
				id,
				Clause.WHERE,
				entityDescriptor().getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();
		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlSelect )
				.translate( jdbcParamBindings, QueryOptions.NONE );

		final List<Object> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
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
					public String getQueryIdentifier(String sql) {
						return sql;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						throw new UnsupportedOperationException( "Follow-on locking not supported yet" );
					}

				},
				(row) -> {
					// because we select the natural-id we want to "reduce" the result
					assert row.length == 1;
					return row[0];
				},
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		if ( results.isEmpty() ) {
			return null;
		}

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Resolving id to natural-id returned more that one row : %s #%s",
							entityDescriptor().getEntityName(),
							id
					)
			);
		}

		return results.get( 0 );
	}
}
