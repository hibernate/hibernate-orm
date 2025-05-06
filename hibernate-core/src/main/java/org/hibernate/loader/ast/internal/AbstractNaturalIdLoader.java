/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.*;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Base support for {@link NaturalIdLoader} implementations
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
	public EntityMappingType getLoadable() {
		return entityDescriptor();
	}

	@Override
	public T load(Object naturalIdValue, NaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();

		final LockOptions lockOptions = options.getLockOptions() == null ? LockOptions.NONE : options.getLockOptions();

		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				getLoadable(),
				// null here means to select everything
				null,
				true,
				emptyList(), // we're going to add the restrictions ourselves
				null,
				1,
				session.getLoadQueryInfluencers(),
				lockOptions,
				JdbcParametersList.newBuilder()::add,
				factory
		);

		// we have to add the restrictions ourselves manually because we want special null handling
		final JdbcParameterBindings bindings = new JdbcParameterBindingsImpl( naturalIdMapping.getJdbcTypeCount() );
		applyNaturalIdRestriction(
				naturalIdMapping().normalizeInput( naturalIdValue ),
				sqlSelect.getQuerySpec().getFromClause().getRoots().get(0),
				sqlSelect.getQuerySpec()::applyPredicate,
				bindings::addBinding,
				new LoaderSqlAstCreationState(
						sqlSelect.getQuerySpec(),
						new SqlAliasBaseManager(),
						new SimpleFromClauseAccessImpl(),
						lockOptions,
						(fetchParent, creationState) -> ImmutableFetchList.EMPTY,
						true,
						new LoadQueryInfluencers( factory ),
						factory.getSqlTranslationEngine()
				),
				session
		);

		final QueryOptions queryOptions = new SimpleQueryOptions( lockOptions, false );
		final JdbcOperationQuerySelect jdbcSelect =
				factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( factory, sqlSelect )
						.translate( bindings, queryOptions );

		final long startToken = factory.getStatistics().isStatisticsEnabled() ? System.nanoTime() : -1;

		final List<T> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				bindings,
				new NaturalIdLoaderWithOptionsExecutionContext( session, queryOptions ),
				RowTransformerSingularReturnImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Loading by natural-id returned more that one row : %s",
							entityDescriptor.getEntityName()
					)
			);
		}

		final T result = results.isEmpty() ? null : results.get(0);
//		entityDescriptor().getPostLoadListener().completedLoad( result, entityDescriptor(), naturalIdValue, KeyType.NATURAL_ID, LoadSource.DATABASE );
		if ( startToken > 0 ) {
			session.getFactory().getStatistics().naturalIdQueryExecuted(
					entityDescriptor().getEntityPersister().getRootEntityName(),
					System.nanoTime() - startToken
			);
//			// todo (6.0) : need a "load-by-natural-id" stat, e.g.,
//			// final Object identifier = entityDescriptor().getIdentifierMapping().getIdentifier( result, session );
//			// session.getFactory().getStatistics().entityLoadedByNaturalId( entityDescriptor(), identifier );
		}
		return result;
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
		final SessionFactoryImplementor factory = session.getFactory();

		final LockOptions lockOptions = options.getLockOptions() != null ? options.getLockOptions() : LockOptions.NONE;

		final NavigablePath entityPath = new NavigablePath( entityDescriptor.getRootPathName() );
		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				fetchProcessor,
				true,
				new LoadQueryInfluencers( factory ),
				factory.getSqlTranslationEngine()
		);

		final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				true,
				entityPath,
				null,
				null,
				() -> rootQuerySpec::applyPredicate,
				sqlAstCreationState
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		final DomainResult<?> domainResult = domainResultProducer.apply( rootTableGroup, sqlAstCreationState );

		final SelectStatement sqlSelect = new SelectStatement( rootQuerySpec, singletonList( domainResult ) );

		final JdbcParameterBindings bindings = new JdbcParameterBindingsImpl( naturalIdMapping.getJdbcTypeCount() );

		applyNaturalIdRestriction(
				bindValue,
				rootTableGroup,
				rootQuerySpec::applyPredicate,
				bindings::addBinding,
				sqlAstCreationState,
				session
		);

		final QueryOptions queryOptions = new SimpleQueryOptions( lockOptions, false );
		final JdbcOperationQuerySelect jdbcSelect =
				factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( factory, sqlSelect )
						.translate( bindings, queryOptions );

		final Long startToken = statementStartHandler.apply( factory.getStatistics().isStatisticsEnabled() );

		final List<L> results = factory.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				bindings,
				new NaturalIdLoaderWithOptionsExecutionContext( session, queryOptions ),
				RowTransformerSingularReturnImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Loading by natural-id returned more that one row : %s",
							entityDescriptor.getEntityName()
					)
			);
		}

		final L result = results.isEmpty() ? null : results.get(0);
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
			@SuppressWarnings("unused") SessionFactoryImplementor sessionFactory) {
		final TableReference tableReference =
				rootTableGroup.getTableReference( rootTableGroup.getNavigablePath(),
						selectableMapping.getContainingTableExpression() );
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
		return sqlExpressionResolver.resolveSqlExpression( tableReference, selectableMapping );
	}

	@Override
	public Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
		return selectByNaturalId(
				naturalIdMapping().normalizeInput( naturalIdValue ),
				NaturalIdLoadOptions.NONE,
				(tableGroup, creationState) -> entityDescriptor.getIdentifierMapping().createDomainResult(
						tableGroup.getNavigablePath().append( EntityIdentifierMapping.ID_ROLE_NAME ),
						tableGroup,
						null,
						creationState
				),
				(fetchParent, creationState) -> ImmutableFetchList.EMPTY,
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
		final SessionFactoryImplementor factory = session.getFactory();

		final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor(),
				singletonList( naturalIdMapping() ),
				entityDescriptor().getIdentifierMapping(),
				null,
				1,
				session.getLoadQueryInfluencers(),
				LockOptions.NONE,
				builder::add,
				factory
		);
		final JdbcParametersList jdbcParameters = builder.build();
		final JdbcParameterBindings bindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final int offset = bindings.registerParametersForEachJdbcValue(
				id,
				entityDescriptor().getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final JdbcOperationQuerySelect jdbcSelect =
				factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( factory, sqlSelect )
						.translate( bindings, QueryOptions.NONE );

		final List<Object> results =
				factory.getJdbcServices().getJdbcSelectExecutor()
						.list(
								jdbcSelect,
								bindings,
								new NoCallbackExecutionContext( session ),
								// because we select the natural-id we want to "reduce" the result
								RowTransformerSingularReturnImpl.instance(),
								null,
								ListResultsConsumer.UniqueSemantic.FILTER,
								1
						);
		return switch ( results.size() ) {
			case 0 -> null;
			case 1 -> results.get( 0 );
			default -> throw new HibernateException(
					String.format(
							"Resolving id to natural-id returned more that one row : %s #%s",
							entityDescriptor().getEntityName(),
							id
					)
			);
		};
	}


	private static class NaturalIdLoaderWithOptionsExecutionContext extends BaseExecutionContext {
		private final Callback callback;
		private final QueryOptions queryOptions;

		public NaturalIdLoaderWithOptionsExecutionContext(SharedSessionContractImplementor session, QueryOptions queryOptions) {
			super( session );
			this.queryOptions = queryOptions;
			callback = new CallbackImpl();
		}

		@Override
		public QueryOptions getQueryOptions() {
			return queryOptions;
		}

		@Override
		public Callback getCallback() {
			return callback;
		}

	}
}
