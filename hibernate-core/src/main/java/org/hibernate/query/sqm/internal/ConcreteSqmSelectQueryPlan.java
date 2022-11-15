/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.ScrollMode;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.Query;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.internal.RowTransformerJpaTupleImpl;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import static org.hibernate.query.sqm.internal.QuerySqmImpl.CRITERIA_HQL_STRING;

/**
 * Standard Hibernate implementation of SelectQueryPlan for SQM-backed
 * {@link Query} implementations, which means
 * HQL/JPQL or {@link jakarta.persistence.criteria.CriteriaQuery}
 *
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement<?> sqm;
	private final DomainParameterXref domainParameterXref;
	private final RowTransformer<R> rowTransformer;
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

		this.rowTransformer = determineRowTransformer( sqm, resultType, tupleMetadata, queryOptions );

		final ListResultsConsumer.UniqueSemantic uniqueSemantic;
		if ( sqm.producesUniqueResults() && !containsCollectionFetches( queryOptions ) ) {
			uniqueSemantic = ListResultsConsumer.UniqueSemantic.NONE;
		}
		else {
			uniqueSemantic = ListResultsConsumer.UniqueSemantic.ALLOW;
		}
		this.listInterpreter = (unused, executionContext, sqmInterpretation, jdbcParameterBindings) -> {
			final SharedSessionContractImplementor session = executionContext.getSession();
			final JdbcSelect jdbcSelect = sqmInterpretation.getJdbcSelect();
			try {
				final SubselectFetch.RegistrationHandler subSelectFetchKeyHandler = SubselectFetch.createRegistrationHandler(
						session.getPersistenceContext().getBatchFetchQueue(),
						sqmInterpretation.selectStatement,
						Collections.emptyList(),
						jdbcParameterBindings
				);

				session.autoFlushIfRequired( jdbcSelect.getAffectedTableNames() );

				return session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
						jdbcSelect,
						jdbcParameterBindings,
						new SqmJdbcExecutionContextAdapter( executionContext, jdbcSelect ) {
							@Override
							public void registerLoadingEntityEntry(EntityKey entityKey, LoadingEntityEntry entry) {
								subSelectFetchKeyHandler.addKey( entityKey, entry );
							}

							@Override
							public String getQueryIdentifier(String sql) {
								if ( CRITERIA_HQL_STRING.equals( hql ) ) {
									return "[CRITERIA] " + sql;
								}
								return hql;
							}

							@Override
							public boolean hasQueryExecutionToBeAddedToStatistics() {
								return true;
							}
						},
						rowTransformer,
						uniqueSemantic
				);
			}
			finally {
				domainParameterXref.clearExpansions();
			}
		};

		this.scrollInterpreter = (scrollMode, executionContext, sqmInterpretation, jdbcParameterBindings) -> {
			try {
//				final SubselectFetch.RegistrationHandler subSelectFetchKeyHandler = SubselectFetch.createRegistrationHandler(
//						executionContext.getSession().getPersistenceContext().getBatchFetchQueue(),
//						sqmInterpretation.selectStatement,
//						Collections.emptyList(),
//						jdbcParameterBindings
//				);

				final JdbcSelectExecutor jdbcSelectExecutor = executionContext.getSession()
						.getFactory()
						.getJdbcServices()
						.getJdbcSelectExecutor();
				return jdbcSelectExecutor.scroll(
						sqmInterpretation.getJdbcSelect(),
						scrollMode,
						jdbcParameterBindings,
						new SqmJdbcExecutionContextAdapter( executionContext, sqmInterpretation.jdbcSelect ),
						rowTransformer
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

	private static boolean containsCollectionFetches(QueryOptions queryOptions) {
		final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
		return appliedGraph != null && appliedGraph.getGraph() != null && containsCollectionFetches( appliedGraph.getGraph() );
	}

	private static boolean containsCollectionFetches(GraphImplementor<?> graph) {
		for ( AttributeNodeImplementor<?> attributeNodeImplementor : graph.getAttributeNodeImplementors() ) {
			if ( attributeNodeImplementor.getAttributeDescriptor().isCollection() ) {
				return true;
			}
			for ( SubGraphImplementor<?> subGraph : attributeNodeImplementor.getSubGraphMap().values() ) {
				if ( containsCollectionFetches( subGraph ) ) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private RowTransformer<R> determineRowTransformer(
			SqmSelectStatement<?> sqm,
			Class<R> resultType,
			TupleMetadata tupleMetadata,
			QueryOptions queryOptions) {
		if ( queryOptions.getTupleTransformer() != null ) {
			return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
		}

		if ( resultType == null ) {
			return RowTransformerStandardImpl.instance();
		}

		if ( resultType.isArray() ) {
			return (RowTransformer<R>) RowTransformerArrayImpl.instance();
		}

		// NOTE : if we get here :
		// 		1) there is no TupleTransformer specified
		// 		2) an explicit result-type, other than an array, was specified

		final List<SqmSelection<?>> selections = sqm.getQueryPart().getFirstQuerySpec().getSelectClause().getSelections();
		if ( tupleMetadata != null ) {
			// resultType is Tuple..
			if ( queryOptions.getTupleTransformer() == null ) {
				return (RowTransformer<R>) new RowTransformerJpaTupleImpl( tupleMetadata );
			}

			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer : " +
							queryOptions.getTupleTransformer()
			);
		}

		// NOTE : if we get here we have a resultType of some kind

		if ( selections.size() > 1 ) {
			throw new IllegalQueryOperationException( "Query defined multiple selections, return cannot be typed (other that Object[] or Tuple)" );
		}
		else {
			return RowTransformerSingularReturnImpl.instance();
		}
	}

	private RowTransformer<R> makeRowTransformerTupleTransformerAdapter(
			SqmSelectStatement<?> sqm,
			QueryOptions queryOptions) {
		final List<String> aliases = new ArrayList<>();
		for ( SqmSelection<?> sqmSelection : sqm.getQuerySpec().getSelectClause().getSelections() ) {
			// The row a tuple transformer gets to see only contains 1 element for a dynamic instantiation
			if ( sqmSelection.getSelectableNode() instanceof SqmDynamicInstantiation<?> ) {
				aliases.add( sqmSelection.getAlias() );
			}
			else {
				sqmSelection.getSelectableNode().visitSubSelectableNodes(
						subSelection -> aliases.add( subSelection.getAlias() )
				);
			}
		}


		@SuppressWarnings("unchecked")
		TupleTransformer<R> tupleTransformer = (TupleTransformer<R>) queryOptions.getTupleTransformer();
		return new RowTransformerTupleTransformerAdapter<R>(
				ArrayHelper.toStringArray( aliases ),
				tupleTransformer
		);
	}

	@Override
	public List<R> performList(DomainQueryExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return Collections.emptyList();
		}
		return withCacheableSqmInterpretation( executionContext, null, listInterpreter );
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, DomainQueryExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return EmptyScrollableResults.INSTANCE;
		}
		return withCacheableSqmInterpretation( executionContext, scrollMode, scrollInterpreter );
	}

	private <T, X> T withCacheableSqmInterpretation(DomainQueryExecutionContext executionContext, X context, SqmInterpreter<T, X> interpreter) {
		// NOTE : VERY IMPORTANT - intentional double-lock checking
		//		The other option would be to leverage `java.util.concurrent.locks.ReadWriteLock`
		//		to protect access.  However, synchronized is much simpler here.  We will verify
		// 		during throughput testing whether this is an issue and consider changes then

		CacheableSqmInterpretation localCopy = cacheableSqmInterpretation;
		JdbcParameterBindings jdbcParameterBindings = null;

		if ( localCopy == null ) {
			synchronized ( this ) {
				localCopy = cacheableSqmInterpretation;
				if ( localCopy == null ) {
					localCopy = buildCacheableSqmInterpretation(
							sqm,
							domainParameterXref,
							executionContext
					);
					jdbcParameterBindings = localCopy.firstParameterBindings;
					localCopy.firstParameterBindings = null;
					cacheableSqmInterpretation = localCopy;
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
				localCopy = buildCacheableSqmInterpretation(
						sqm,
						domainParameterXref,
						executionContext
				);
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
		final SharedSessionContractImplementor session = executionContext.getSession();
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				sqmInterpretation.getJdbcParamsXref(),
				session.getFactory().getRuntimeMetamodels().getMappingMetamodel(),
				sqmInterpretation.getTableGroupAccess()::findTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					//this is pretty ugly!
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmInterpretation.getSqmParameterMappingModelTypes().get(parameter);
					}
				},
				session
		);
		sqmInterpretation.getJdbcSelect().bindFilterJdbcParameters( jdbcParameterBindings );
		return jdbcParameterBindings;
	}

	private static CacheableSqmInterpretation buildCacheableSqmInterpretation(
			SqmSelectStatement<?> sqm,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final QueryEngine queryEngine = sessionFactory.getQueryEngine();

		final SqmTranslatorFactory sqmTranslatorFactory = queryEngine.getSqmTranslatorFactory();

		final SqmTranslator<SelectStatement> sqmConverter = sqmTranslatorFactory.createSelectTranslator(
				sqm,
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getQueryParameterBindings(),
				executionContext.getSession().getLoadQueryInfluencers(),
				sessionFactory,
				true
		);

//			tableGroupAccess = sqmConverter.getFromClauseAccess();
		final SqmTranslation<SelectStatement> sqmInterpretation = sqmConverter.translate();
		final FromClauseAccess tableGroupAccess = sqmConverter.getFromClauseAccess();

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcSelect> selectTranslator = sqlAstTranslatorFactory.buildSelectTranslator(
				sessionFactory,
				sqmInterpretation.getSqlAst()
		);

		final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<List<JdbcParameter>>>> jdbcParamsXref
				= SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmInterpretation::getJdbcParamsBySqmParam );
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				session.getFactory().getRuntimeMetamodels().getMappingMetamodel(),
				tableGroupAccess::findTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmInterpretation.getSqmParameterMappingModelTypeResolutions().get(parameter);
					}
				},
				session
		);
		final JdbcSelect jdbcSelect = selectTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		return new CacheableSqmInterpretation(
				sqmInterpretation.getSqlAst(),
				jdbcSelect,
				tableGroupAccess,
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
		private final JdbcSelect jdbcSelect;
		private final FromClauseAccess tableGroupAccess;
		private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<List<JdbcParameter>>>> jdbcParamsXref;
		private final Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes;
		private transient JdbcParameterBindings firstParameterBindings;

		CacheableSqmInterpretation(
				SelectStatement selectStatement,
				JdbcSelect jdbcSelect,
				FromClauseAccess tableGroupAccess,
				Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<List<JdbcParameter>>>> jdbcParamsXref,
				Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes,
				JdbcParameterBindings firstParameterBindings) {
			this.selectStatement = selectStatement;
			this.jdbcSelect = jdbcSelect;
			this.tableGroupAccess = tableGroupAccess;
			this.jdbcParamsXref = jdbcParamsXref;
			this.sqmParameterMappingModelTypes = sqmParameterMappingModelTypes;
			this.firstParameterBindings = firstParameterBindings;
		}

		SelectStatement getSelectStatement() {
			return selectStatement;
		}

		JdbcSelect getJdbcSelect() {
			return jdbcSelect;
		}

		FromClauseAccess getTableGroupAccess() {
			return tableGroupAccess;
		}

		Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<List<JdbcParameter>>>> getJdbcParamsXref() {
			return jdbcParamsXref;
		}

		public Map<SqmParameter<?>, MappingModelExpressible<?>> getSqmParameterMappingModelTypes() {
			return sqmParameterMappingModelTypes;
		}

		JdbcParameterBindings getFirstParameterBindings() {
			return firstParameterBindings;
		}

		void setFirstParameterBindings(JdbcParameterBindings firstParameterBindings) {
			this.firstParameterBindings = firstParameterBindings;
		}
	}
}
