/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.*;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.stat.spi.StatisticsImplementor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hibernate.engine.internal.NaturalIdLogging.NATURAL_ID_LOGGER;

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

		final LockOptions lockOptions = options.getLockOptions() == null
				? new LockOptions()
				: options.getLockOptions();

		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				getLoadable(),
				null,  // null here means to select everything
				true,
				emptyList(),  // we're going to add the restrictions ourselves
				null,
				1,
				session.getLoadQueryInfluencers(),
				lockOptions,
				JdbcParametersList.newBuilder()::add,
				factory
		);

		final QuerySpec querySpec = sqlSelect.getQuerySpec();
		return executeNaturalIdQuery(
				naturalIdValue,
				lockOptions,
				sqlSelect,
				querySpec.getFromClause().getRoots().get(0),
				querySpec::applyPredicate,
				new LoaderSqlAstCreationState(
						querySpec,
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
	}

	/**
	 * Apply restriction necessary to match the given natural-id value.
	 * Should also apply any predicates to the predicate consumer and
	 * any parameter / binding pairs.
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
			SqlExpressionResolver sqlExpressionResolver) {
		final TableReference tableReference =
				rootTableGroup.getTableReference( rootTableGroup.getNavigablePath(),
						selectableMapping.getContainingTableExpression() );
		if ( tableReference == null ) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Unable to locate TableReference for '%s' : %s",
							selectableMapping.getContainingTableExpression(),
							rootTableGroup
					)
			);
		}
		return sqlExpressionResolver.resolveSqlExpression( tableReference, selectableMapping );
	}

	@Override
	public Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
		if ( NATURAL_ID_LOGGER.isTraceEnabled() ) {
			NATURAL_ID_LOGGER.retrievingIdForNaturalId(
					entityDescriptor.getEntityName(),
					naturalIdValue instanceof Object[] array
							? Arrays.toString( array )
							: naturalIdValue
			);
		}

		final SessionFactoryImplementor factory = session.getFactory();
		final NavigablePath entityPath = new NavigablePath( entityDescriptor.getRootPathName() );
		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				LockOptions.NONE,
				(fetchParent, creationState) -> ImmutableFetchList.EMPTY,
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

		final DomainResult<?> domainResult =
				entityDescriptor.getIdentifierMapping().createDomainResult(
						rootTableGroup.getNavigablePath()
								.append( EntityIdentifierMapping.ID_ROLE_NAME ),
						rootTableGroup,
						null,
						sqlAstCreationState
				);

		return executeNaturalIdQuery(
				naturalIdValue,
				LockOptions.NONE,
				new SelectStatement( rootQuerySpec, singletonList( domainResult ) ),
				rootTableGroup,
				rootQuerySpec::applyPredicate,
				sqlAstCreationState,
				session
		);
	}

	protected <R> R executeNaturalIdQuery(
			Object naturalIdValue,
			LockOptions lockOptions,
			SelectStatement sqlSelect,
			TableGroup rootTableGroup,
			Consumer<Predicate> predicateConsumer,
			LoaderSqlAstCreationState sqlAstCreationState,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();

		final JdbcParameterBindings bindings =
				new JdbcParameterBindingsImpl( naturalIdMapping.getJdbcTypeCount() );
		applyNaturalIdRestriction(
				naturalIdMapping().normalizeInput( naturalIdValue ),
				rootTableGroup,
				predicateConsumer,
				bindings::addBinding,
				sqlAstCreationState,
				session
		);

		final QueryOptions queryOptions = new SimpleQueryOptions( lockOptions, false );
		final var jdbcSelect = createJdbcOperationQuerySelect( sqlSelect, factory, bindings, queryOptions );

		final StatisticsImplementor statistics = factory.getStatistics();
		final boolean statisticsEnabled = statistics.isStatisticsEnabled();
		final long startTime = statisticsEnabled ? System.nanoTime() : -1L;

		final List<R> results =
				factory.getJdbcServices().getJdbcSelectExecutor()
						.list(
								jdbcSelect,
								bindings,
								new NaturalIdLoaderWithOptionsExecutionContext( session, queryOptions ),
								RowTransformerSingularReturnImpl.instance(),
								null,
								ListResultsConsumer.UniqueSemantic.FILTER,
								1
						);

		if ( statisticsEnabled ) {
			statistics.naturalIdQueryExecuted(
					entityDescriptor().getEntityPersister().getRootEntityName(),
					System.nanoTime() - startTime
			);
		}

		return switch ( results.size() ) {
			case 0 -> null;
			case 1 -> results.get( 0 );
			default -> throw new HibernateException(
					String.format(
							"Query by natural id returned more that one row: %s",
							entityDescriptor.getEntityName()
					)
			);
		};
	}

	private static JdbcOperationQuerySelect createJdbcOperationQuerySelect(
			SelectStatement sqlSelect,
			SessionFactoryImplementor factory,
			JdbcParameterBindings bindings,
			QueryOptions queryOptions) {
		return factory.getJdbcServices().getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory, sqlSelect )
				.translate( bindings, queryOptions );
	}

	@Override
	public Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		final EntityIdentifierMapping identifierMapping = entityDescriptor().getIdentifierMapping();

		final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor(),
				singletonList( naturalIdMapping() ),
				identifierMapping,
				null,
				1,
				session.getLoadQueryInfluencers(),
				new LockOptions(),
				builder::add,
				factory
		);
		final JdbcParametersList jdbcParameters = builder.build();
		final JdbcParameterBindings bindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final int offset = bindings.registerParametersForEachJdbcValue( id, identifierMapping, jdbcParameters, session );
		assert offset == jdbcParameters.size();

		final List<?> results =
				factory.getJdbcServices().getJdbcSelectExecutor()
						.list(
								createJdbcOperationQuerySelect( sqlSelect, factory, bindings, QueryOptions.NONE ),
								bindings,
								new NoCallbackExecutionContext( session ),
								// because we select the natural id,
								// we want to "reduce" the result
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
							"Resolving id to natural id returned more that one row: %s #%s",
							entityDescriptor().getEntityName(),
							id
					)
			);
		};
	}

	void applyRestriction(
			TableGroup rootTableGroup,
			Consumer<Predicate> predicateConsumer,
			BiConsumer<JdbcParameter, JdbcParameterBinding> jdbcParameterConsumer,
			Object jdbcValue,
			SelectableMapping jdbcValueMapping,
			SqlExpressionResolver expressionResolver) {
		final Expression columnReference =
				resolveColumnReference( rootTableGroup, jdbcValueMapping, expressionResolver );
		if ( jdbcValue == null ) {
			predicateConsumer.accept( new NullnessPredicate( columnReference ) );
		}
		else {
			final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcValueMapping.getJdbcMapping() );
			final ComparisonPredicate predicate =
					new ComparisonPredicate( columnReference, ComparisonOperator.EQUAL, jdbcParameter );
			predicateConsumer.accept( predicate );
			jdbcParameterConsumer.accept( jdbcParameter,
					new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), jdbcValue ) );
		}
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
