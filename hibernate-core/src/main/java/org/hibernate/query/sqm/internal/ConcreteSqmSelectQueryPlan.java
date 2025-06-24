/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Tuple;

import org.hibernate.AssertionFailure;
import org.hibernate.InstantiationException;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.internal.RowTransformerCheckingImpl;
import org.hibernate.sql.results.internal.RowTransformerConstructorImpl;
import org.hibernate.sql.results.internal.RowTransformerJpaTupleImpl;
import org.hibernate.sql.results.internal.RowTransformerListImpl;
import org.hibernate.sql.results.internal.RowTransformerMapImpl;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.ReflectHelper.isClass;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.query.sqm.internal.AppliedGraphs.containsCollectionFetches;
import static org.hibernate.query.sqm.internal.SqmQueryImpl.CRITERIA_HQL_STRING;
import static org.hibernate.query.sqm.internal.SqmUtil.generateJdbcParamsXref;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelectionAssignableToResultType;

/**
 * Standard implementation of {@link SelectQueryPlan} for SQM-backed
 * implementations of {@link org.hibernate.query.Query}, that is, for
 * HQL/JPQL or for {@link jakarta.persistence.criteria.CriteriaQuery}.
 *
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement<?> sqm;
	private final DomainParameterXref domainParameterXref;
	private final SqmInterpreter<?, ? extends ResultsConsumer<?, R>> executeQueryInterpreter;
	private final SqmInterpreter<List<R>, Void> listInterpreter;
	private final SqmInterpreter<ScrollableResultsImplementor<R>, ScrollMode> scrollInterpreter;

	private volatile CacheableSqmInterpretation cacheableSqmInterpretation;

	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement<?> sqm,
			String hql,
			DomainParameterXref domainParameterXref,
			Class<R> resultType,
			TupleMetadata tupleMetadata,
			QueryOptions queryOptions) {
		this.sqm = sqm;
		this.domainParameterXref = domainParameterXref;

		final ListResultsConsumer.UniqueSemantic uniqueSemantic =
				sqm.producesUniqueResults() && !containsCollectionFetches( queryOptions )
						? ListResultsConsumer.UniqueSemantic.NONE
						: ListResultsConsumer.UniqueSemantic.ALLOW;
		this.executeQueryInterpreter = (resultsConsumer, executionContext, sqmInterpretation, jdbcParameterBindings) -> {
			final SharedSessionContractImplementor session = executionContext.getSession();
			final JdbcOperationQuerySelect jdbcSelect = sqmInterpretation.getJdbcSelect();
			try {
				final SubselectFetch.RegistrationHandler subSelectFetchKeyHandler = SubselectFetch.createRegistrationHandler(
						session.getPersistenceContext().getBatchFetchQueue(),
						sqmInterpretation.selectStatement,
						JdbcParametersList.empty(),
						jdbcParameterBindings
				);
				session.autoFlushIfRequired( jdbcSelect.getAffectedTableNames(), true );
				final Expression fetchExpression = sqmInterpretation.selectStatement.getQueryPart()
						.getFetchClauseExpression();
				final int resultCountEstimate = fetchExpression != null
						? interpretIntExpression( fetchExpression, jdbcParameterBindings )
						: -1;
				return session.getFactory().getJdbcServices().getJdbcSelectExecutor().executeQuery(
						jdbcSelect,
						jdbcParameterBindings,
						listInterpreterExecutionContext( hql, executionContext, jdbcSelect, subSelectFetchKeyHandler ),
						determineRowTransformer( sqm, resultType, tupleMetadata, executionContext.getQueryOptions() ),
						null,
						resultCountEstimate,
						resultsConsumer
				);
			}
			finally {
				domainParameterXref.clearExpansions();
			}
		};
		this.listInterpreter = (unused, executionContext, sqmInterpretation, jdbcParameterBindings) -> {
			final SharedSessionContractImplementor session = executionContext.getSession();
			final JdbcOperationQuerySelect jdbcSelect = sqmInterpretation.getJdbcSelect();
			try {
				final SubselectFetch.RegistrationHandler subSelectFetchKeyHandler = SubselectFetch.createRegistrationHandler(
						session.getPersistenceContext().getBatchFetchQueue(),
						sqmInterpretation.selectStatement,
						JdbcParametersList.empty(),
						jdbcParameterBindings
				);
				session.autoFlushIfRequired( jdbcSelect.getAffectedTableNames(), true );
				final Expression fetchExpression =
						sqmInterpretation.selectStatement.getQueryPart()
								.getFetchClauseExpression();
				final int resultCountEstimate = fetchExpression != null
						? interpretIntExpression( fetchExpression, jdbcParameterBindings )
						: -1;
				//noinspection unchecked
				return session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
						jdbcSelect,
						jdbcParameterBindings,
						listInterpreterExecutionContext( hql, executionContext, jdbcSelect, subSelectFetchKeyHandler ),
						determineRowTransformer( sqm, resultType, tupleMetadata, executionContext.getQueryOptions() ),
						(Class<R>) executionContext.getResultType(),
						uniqueSemantic,
						resultCountEstimate
				);
			}
			finally {
				domainParameterXref.clearExpansions();
			}
		};

		this.scrollInterpreter = (scrollMode, executionContext, sqmInterpretation, jdbcParameterBindings) -> {
			final SharedSessionContractImplementor session = executionContext.getSession();
			final JdbcOperationQuerySelect jdbcSelect = sqmInterpretation.getJdbcSelect();
			try {
//				final SubselectFetch.RegistrationHandler subSelectFetchKeyHandler = SubselectFetch.createRegistrationHandler(
//						executionContext.getSession().getPersistenceContext().getBatchFetchQueue(),
//						sqmInterpretation.selectStatement,
//						Collections.emptyList(),
//						jdbcParameterBindings
//				);

				final JdbcSelectExecutor jdbcSelectExecutor =
						session.getFactory().getJdbcServices().getJdbcSelectExecutor();
				session.autoFlushIfRequired( jdbcSelect.getAffectedTableNames(), true );
				final Expression fetchExpression =
						sqmInterpretation.selectStatement.getQueryPart()
								.getFetchClauseExpression();
				final int resultCountEstimate = fetchExpression != null
						? interpretIntExpression( fetchExpression, jdbcParameterBindings )
						: -1;
				return jdbcSelectExecutor.scroll(
						jdbcSelect,
						scrollMode,
						jdbcParameterBindings,
						new SqmJdbcExecutionContextAdapter( executionContext, jdbcSelect ),
						determineRowTransformer( sqm, resultType, tupleMetadata, executionContext.getQueryOptions() ),
						resultCountEstimate
				);
			}
			finally {
				domainParameterXref.clearExpansions();
			}
		};

		// todo (6.0) : we should do as much of the building as we can here
		//  	since this is the thing cached, all the work we do here will
		//  	be cached as well.
		// NOTE : this statement ^^ is not affected by load-query-influencers,
		//		multi-valued parameter expansion, etc - because those all
		//		cause the plan to not be cached.
		// NOTE2 (regarding NOTE) : not sure multi-valued parameter expansion, in
		//		particular, should veto caching of the plan.  The expansion happens
		//		for each execution - see creation of `JdbcParameterBindings` in
		//		`#performList` and `#performScroll`.
	}

	protected static SqmJdbcExecutionContextAdapter listInterpreterExecutionContext(
			String hql,
			DomainQueryExecutionContext executionContext,
			JdbcOperationQuerySelect jdbcSelect,
			SubselectFetch.RegistrationHandler subSelectFetchKeyHandler) {
		return new MySqmJdbcExecutionContextAdapter( executionContext, jdbcSelect, subSelectFetchKeyHandler, hql );
	}

	protected static int interpretIntExpression(Expression expression, JdbcParameterBindings jdbcParameterBindings) {
		if ( expression instanceof Literal literal ) {
			return ( (Number) literal.getLiteralValue() ).intValue();
		}
		else if ( expression instanceof JdbcParameter jdbcParameter ) {
			return (int) jdbcParameterBindings.getBinding( jdbcParameter ).getBindValue();
		}
		else if ( expression instanceof SqmParameterInterpretation parameterInterpretation ) {
			final JdbcParameter jdbcParameter = (JdbcParameter) parameterInterpretation.getResolvedExpression();
			return (int) jdbcParameterBindings.getBinding( jdbcParameter ).getBindValue();
		}
		else {
			return -1;
		}
	}

	private static List<SqmSelection<?>> selections(SqmSelectStatement<?> sqm) {
		return sqm.getQueryPart().getFirstQuerySpec().getSelectClause().getSelections();
	}

	private static final Map<Class<?>, Class<?>> WRAPPERS
			= Map.of(
					boolean.class, Boolean.class,
					int.class, Integer.class,
					long.class, Long.class,
					short.class, Short.class,
					byte.class, Byte.class,
					float.class, Float.class,
					double.class, Double.class,
					char.class, Character.class
			);

	/**
	 * If the result type of the query is {@link Tuple}, {@link Map}, {@link List},
	 * or any record or class type with an appropriate constructor, then we attempt
	 * to repackage the result tuple as an instance of the result type using an
	 * appropriate {@link RowTransformer}.
	 *
	 * @param resultClass The requested result type of the query
	 * @return A {@link RowTransformer} responsible for repackaging the result type
	 */
	protected static <T> RowTransformer<T> determineRowTransformer(
			SqmSelectStatement<?> sqm,
			Class<T> resultClass,
			TupleMetadata tupleMetadata,
			QueryOptions queryOptions) {
		if ( queryOptions.getTupleTransformer() != null ) {
			return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
		}
		else if ( resultClass == null || resultClass == Object.class ) {
			return RowTransformerStandardImpl.instance();
		}
		else {
			final var selections = selections( sqm );
			if ( selections == null ) {
				throw new AssertionFailure( "No selections" );
			}
			else {
				final Class<T> resultType = primitiveToWrapper( resultClass );
				return switch ( selections.size() ) {
					case 0 -> throw new AssertionFailure( "No selections" );
					case 1 -> singleItemRowTransformer( sqm, tupleMetadata, selections.get( 0 ), resultType );
					default -> multipleItemRowTransformer( sqm, tupleMetadata, resultType );
				};
			}
		}
	}

	/**
	 * We tolerate the use of primitive query result types, for example,
	 * {@code long.class} instead of {@code Long.class}. Note that this
	 * has no semantics: we don't attempt to enforce that the query
	 * result is non-null if it is primitive.
	 */
	@SuppressWarnings("unchecked")
	private static <T> Class<T> primitiveToWrapper(Class<T> resultClass) {
		// this cast, which looks like complete nonsense, is perfectly correct,
		// since Java assigns the type Class<Long> to te expression long.class
		// even though the resulting class object is distinct from Long.class
		return (Class<T>) WRAPPERS.getOrDefault( resultClass, resultClass );
	}

	@SuppressWarnings("unchecked")
	private static <T> RowTransformer<T> multipleItemRowTransformer
			(SqmSelectStatement<?> sqm, TupleMetadata tupleMetadata, Class<T> resultType) {
		if ( resultType.isArray() ) {
			return (RowTransformer<T>) RowTransformerArrayImpl.instance();
		}
		else if ( List.class.equals( resultType ) ) {
			return (RowTransformer<T>) RowTransformerListImpl.instance();
		}
		else if ( Tuple.class.equals( resultType ) ) {
			return (RowTransformer<T>) new RowTransformerJpaTupleImpl( tupleMetadata );
		}
		else if ( Map.class.equals( resultType ) ) {
			return (RowTransformer<T>) new RowTransformerMapImpl( tupleMetadata );
		}
		else if ( isClass( resultType ) ) {
			return new RowTransformerConstructorImpl<>( resultType, tupleMetadata,
					sqm.nodeBuilder().getTypeConfiguration() );
		}
		else {
			throw new QueryTypeMismatchException( "Result type '" + resultType.getSimpleName()
												+ "' cannot be used to package the selected expressions" );
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> RowTransformer<T> singleItemRowTransformer
			(SqmSelectStatement<?> sqm, TupleMetadata tupleMetadata, SqmSelection<?> selection, Class<T> resultType) {
		if ( isSelectionAssignableToResultType( selection, resultType ) ) {
			return RowTransformerSingularReturnImpl.instance();
		}
		else if ( resultType.isArray() ) {
			return (RowTransformer<T>) RowTransformerArrayImpl.instance();
		}
		else if ( List.class.equals( resultType ) ) {
			return (RowTransformer<T>) RowTransformerListImpl.instance();
		}
		else if ( Tuple.class.equals( resultType ) ) {
			return (RowTransformer<T>) new RowTransformerJpaTupleImpl( tupleMetadata );
		}
		else if ( Map.class.equals( resultType ) ) {
			return (RowTransformer<T>) new RowTransformerMapImpl( tupleMetadata );
		}
		else if ( isClass( resultType ) ) {
			try {
				return new RowTransformerConstructorImpl<>( resultType, tupleMetadata,
						sqm.nodeBuilder().getTypeConfiguration() );
			}
			catch (InstantiationException ie) {
				return new RowTransformerCheckingImpl<>( resultType );
			}
		}
		else {
			return new RowTransformerCheckingImpl<>( resultType );
		}
	}

	private static <T> RowTransformer<T> makeRowTransformerTupleTransformerAdapter(
			SqmSelectStatement<?> sqm,
			QueryOptions queryOptions) {
		@SuppressWarnings("unchecked")
		final TupleTransformer<T> tupleTransformer = (TupleTransformer<T>) queryOptions.getTupleTransformer();
		return new RowTransformerTupleTransformerAdapter<>( adapterAliases( sqm ), tupleTransformer );
	}

	private static String[] adapterAliases(SqmSelectStatement<?> sqm) {
		final List<String> aliases = new ArrayList<>();
		for ( SqmSelection<?> sqmSelection : sqm.getQuerySpec().getSelectClause().getSelections() ) {
			// The row a tuple transformer gets to see only contains 1 element for a dynamic instantiation
			final SqmSelectableNode<?> selectableNode = sqmSelection.getSelectableNode();
			if ( selectableNode instanceof SqmDynamicInstantiation<?> ) {
				aliases.add( sqmSelection.getAlias() );
			}
			else {
				selectableNode.visitSubSelectableNodes( subSelection -> aliases.add( subSelection.getAlias() ) );
			}
		}
		return toStringArray( aliases );
	}

	@Override
	public <T> T executeQuery(DomainQueryExecutionContext executionContext, ResultsConsumer<T, R> resultsConsumer) {
		@SuppressWarnings("unchecked") //TODO: check the return type
		var interpreter = (SqmInterpreter<T, ResultsConsumer<T, R>>) executeQueryInterpreter;
		return withCacheableSqmInterpretation( executionContext, resultsConsumer, interpreter );
	}

	@Override
	public List<R> performList(DomainQueryExecutionContext executionContext) {
		return executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0
				? emptyList()
				: withCacheableSqmInterpretation( executionContext, null, listInterpreter );
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, DomainQueryExecutionContext executionContext) {
		return executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0
				? EmptyScrollableResults.instance()
				: withCacheableSqmInterpretation( executionContext, scrollMode, scrollInterpreter );
	}

	private <T, X> T withCacheableSqmInterpretation(DomainQueryExecutionContext executionContext, X context, SqmInterpreter<T, X> interpreter) {
		// NOTE : VERY IMPORTANT - intentional double-lock checking
		//		The other option would be to leverage `java.util.concurrent.locks.ReadWriteLock`
		//		to protect access.  However, synchronized is much simpler here.  We will verify
		// 		during throughput testing whether this is an issue and consider changes then

		CacheableSqmInterpretation localCopy = cacheableSqmInterpretation;
		JdbcParameterBindings jdbcParameterBindings = null;

		executionContext.getSession().autoPreFlush();

		if ( localCopy == null ) {
			synchronized ( this ) {
				localCopy = cacheableSqmInterpretation;
				if ( localCopy == null ) {
					localCopy = buildCacheableSqmInterpretation( sqm, domainParameterXref, executionContext );
					jdbcParameterBindings = localCopy.firstParameterBindings;
					localCopy.firstParameterBindings = null;
					cacheableSqmInterpretation = localCopy;
				}
				else {
					// If the translation depends on parameter bindings or it isn't compatible with the current query options,
					// we have to rebuild the JdbcSelect, which is still better than having to translate from SQM to SQL AST again
					if ( localCopy.jdbcSelect.dependsOnParameterBindings() ) {
						jdbcParameterBindings = createJdbcParameterBindings( localCopy, executionContext );
					}
					// If the translation depends on the limit or lock options, we have to rebuild the JdbcSelect
					// We could avoid this by putting the lock options into the cache key
					if ( !localCopy.jdbcSelect.isCompatibleWith( jdbcParameterBindings, executionContext.getQueryOptions() ) ) {
						localCopy = buildCacheableSqmInterpretation( sqm, domainParameterXref, executionContext );
						jdbcParameterBindings = localCopy.firstParameterBindings;
						localCopy.firstParameterBindings = null;
						cacheableSqmInterpretation = localCopy;
					}
				}
			}
		}
		else {
			// If the translation depends on parameter bindings or it isn't compatible with the current query options,
			// we have to rebuild the JdbcSelect, which is still better than having to translate from SQM to SQL AST again
			if ( localCopy.jdbcSelect.dependsOnParameterBindings() ) {
				jdbcParameterBindings = createJdbcParameterBindings( localCopy, executionContext );
			}
			// If the translation depends on the limit or lock options, we have to rebuild the JdbcSelect
			// We could avoid this by putting the lock options into the cache key
			if ( !localCopy.jdbcSelect.isCompatibleWith( jdbcParameterBindings, executionContext.getQueryOptions() ) ) {
				localCopy = buildCacheableSqmInterpretation( sqm, domainParameterXref, executionContext );
				jdbcParameterBindings = localCopy.firstParameterBindings;
				localCopy.firstParameterBindings = null;
				cacheableSqmInterpretation = localCopy;
			}
		}

		if ( jdbcParameterBindings == null ) {
			jdbcParameterBindings = createJdbcParameterBindings( localCopy, executionContext );
		}

		return interpreter.interpret( context, executionContext, localCopy, jdbcParameterBindings );
	}

	private JdbcParameterBindings createJdbcParameterBindings(CacheableSqmInterpretation sqmInterpretation, DomainQueryExecutionContext executionContext) {
		return SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				sqmInterpretation.getJdbcParamsXref(),
				new SqmParameterMappingModelResolutionAccess() {
					//this is pretty ugly!
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmInterpretation.getSqmParameterMappingModelTypes().get(parameter);
					}
				},
				executionContext.getSession()
		);
	}

	private static CacheableSqmInterpretation buildCacheableSqmInterpretation(
			SqmSelectStatement<?> sqm,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final SqmTranslation<SelectStatement> sqmInterpretation =
				sessionFactory.getQueryEngine().getSqmTranslatorFactory()
						.createSelectTranslator(
								sqm,
								executionContext.getQueryOptions(),
								domainParameterXref,
								executionContext.getQueryParameterBindings(),
								executionContext.getSession().getLoadQueryInfluencers(),
								sessionFactory.getSqlTranslationEngine(),
								true
						)
						.translate();

		final SqlAstTranslator<JdbcOperationQuerySelect> selectTranslator =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, sqmInterpretation.getSqlAst() );

		final var jdbcParamsXref =
				generateJdbcParamsXref( domainParameterXref, sqmInterpretation::getJdbcParamsBySqmParam );

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>)
								sqmInterpretation.getSqmParameterMappingModelTypeResolutions().get( parameter );
					}
				},
				session
		);

		return new CacheableSqmInterpretation(
				sqmInterpretation.getSqlAst(),
				selectTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() ),
				jdbcParamsXref,
				sqmInterpretation.getSqmParameterMappingModelTypeResolutions(),
				jdbcParameterBindings
		);
	}

	private interface SqmInterpreter<T, X> {
		T interpret(
				X context,
				DomainQueryExecutionContext executionContext,
				CacheableSqmInterpretation sqmInterpretation,
				JdbcParameterBindings jdbcParameterBindings);
	}

	private static class CacheableSqmInterpretation {
		private final SelectStatement selectStatement;
		private final JdbcOperationQuerySelect jdbcSelect;
		private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
		private final Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes;
		private transient JdbcParameterBindings firstParameterBindings;

		CacheableSqmInterpretation(
				SelectStatement selectStatement,
				JdbcOperationQuerySelect jdbcSelect,
				Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref,
				Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes,
				JdbcParameterBindings firstParameterBindings) {
			this.selectStatement = selectStatement;
			this.jdbcSelect = jdbcSelect;
			this.jdbcParamsXref = jdbcParamsXref;
			this.sqmParameterMappingModelTypes = sqmParameterMappingModelTypes;
			this.firstParameterBindings = firstParameterBindings;
		}

		JdbcOperationQuerySelect getJdbcSelect() {
			return jdbcSelect;
		}

		Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> getJdbcParamsXref() {
			return jdbcParamsXref;
		}

		public Map<SqmParameter<?>, MappingModelExpressible<?>> getSqmParameterMappingModelTypes() {
			return sqmParameterMappingModelTypes;
		}
	}

	private static class MySqmJdbcExecutionContextAdapter extends SqmJdbcExecutionContextAdapter {
		private final SubselectFetch.RegistrationHandler subSelectFetchKeyHandler;
		private final String hql;

		public MySqmJdbcExecutionContextAdapter(
				DomainQueryExecutionContext executionContext,
				JdbcOperationQuerySelect jdbcSelect,
				SubselectFetch.RegistrationHandler subSelectFetchKeyHandler,
				String hql) {
			super( executionContext, jdbcSelect );
			this.subSelectFetchKeyHandler = subSelectFetchKeyHandler;
			this.hql = hql;
		}

		@Override
		public void registerLoadingEntityHolder(EntityHolder holder) {
			subSelectFetchKeyHandler.addKey( holder );
		}

		@Override
		public String getQueryIdentifier(String sql) {
			return CRITERIA_HQL_STRING.equals( hql ) ? "[CRITERIA] " + sql : hql;
		}
	}
}
