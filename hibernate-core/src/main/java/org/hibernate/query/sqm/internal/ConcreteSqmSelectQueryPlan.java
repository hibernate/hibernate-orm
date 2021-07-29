/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.ScrollMode;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.internal.util.streams.StingArrayCollector;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.spi.SqlOmittingQueryOptions;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerJpaTupleImpl;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * Standard Hibernate implementation of SelectQueryPlan for SQM-backed
 * {@link org.hibernate.query.Query} implementations, which means
 * HQL/JPQL or {@link javax.persistence.criteria.CriteriaQuery}
 *
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement sqm;
	private final DomainParameterXref domainParameterXref;
	private final RowTransformer<R> rowTransformer;
	private final SqmInterpreter<List<R>, Void> listInterpreter;
	private final SqmInterpreter<ScrollableResultsImplementor<R>, ScrollMode> scrollInterpreter;

	private volatile CacheableSqmInterpretation cacheableSqmInterpretation;

	@SuppressWarnings("WeakerAccess")
	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement sqm,
			DomainParameterXref domainParameterXref,
			Class<R> resultType,
			QueryOptions queryOptions) {
		this.sqm = sqm;
		this.domainParameterXref = domainParameterXref;

		this.rowTransformer = determineRowTransformer( sqm, resultType, queryOptions );
		this.listInterpreter = (unused, executionContext, sqmInterpretation, jdbcParameterBindings) -> {
			final SharedSessionContractImplementor session = executionContext.getSession();
			final JdbcSelect jdbcSelect = sqmInterpretation.getJdbcSelect();
			try {
				session.autoFlushIfRequired( jdbcSelect.getAffectedTableNames() );
				return session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
						jdbcSelect,
						jdbcParameterBindings,
						SqlOmittingQueryOptions.omitSqlQueryOptions( executionContext, jdbcSelect ),
						rowTransformer,
						ListResultsConsumer.UniqueSemantic.FILTER
				);
			}
			finally {
				domainParameterXref.clearExpansions();
			}
		};
		this.scrollInterpreter = (scrollMode, executionContext, sqmInterpretation, jdbcParameterBindings) -> {
			try {
				return executionContext.getSession().getFactory().getJdbcServices().getJdbcSelectExecutor().scroll(
						sqmInterpretation.getJdbcSelect(),
						scrollMode,
						jdbcParameterBindings,
						executionContext,
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

	@SuppressWarnings("unchecked")
	private RowTransformer<R> determineRowTransformer(
			SqmSelectStatement sqm,
			Class<R> resultType,
			QueryOptions queryOptions) {
		if ( resultType == null || resultType.isArray() ) {
			if ( queryOptions.getTupleTransformer() != null ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
			}
			else {
				return RowTransformerPassThruImpl.instance();
			}
		}

		// NOTE : if we get here, a result-type of some kind (other than Object[].class) was specified

		final List<SqmSelection<?>> selections = sqm.getQueryPart().getFirstQuerySpec().getSelectClause().getSelections();
		if ( Tuple.class.isAssignableFrom( resultType ) ) {
			// resultType is Tuple..
			if ( queryOptions.getTupleTransformer() == null ) {
				final Map<TupleElement<?>, Integer> tupleElementMap = new IdentityHashMap<>( selections.size() );
				for ( int i = 0; i < selections.size(); i++ ) {
					final SqmSelection<?> selection = selections.get( i );
					tupleElementMap.put( selection.getSelectableNode(), i );
				}
				return (RowTransformer<R>) new RowTransformerJpaTupleImpl( new TupleMetadata( tupleElementMap ) );
			}

			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer : " +
							queryOptions.getTupleTransformer()
			);
		}

		// NOTE : if we get here we have a resultType of some kind

		if ( queryOptions.getTupleTransformer() != null ) {
			// aside from checking the type parameters for the given TupleTransformer
			// there is not a decent way to verify that the TupleTransformer returns
			// the same type.  We rely on the API here and assume the best
			return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
		}
		else if ( selections.size() > 1 ) {
			throw new IllegalQueryOperationException( "Query defined multiple selections, return cannot be typed (other that Object[] or Tuple)" );
		}
		else {
			return RowTransformerSingularReturnImpl.instance();
		}
	}

	private RowTransformer<R> makeRowTransformerTupleTransformerAdapter(
			SqmSelectStatement sqm,
			QueryOptions queryOptions) {
		return new RowTransformerTupleTransformerAdapter<>(
				sqm.getQuerySpec().getSelectClause().getSelections()
						.stream()
						.map( SqmSelection::getAlias )
						.collect( StingArrayCollector.INSTANCE ),
				queryOptions.getTupleTransformer()
		);
	}

	@Override
	public List<R> performList(ExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return Collections.emptyList();
		}
		return withCacheableSqmInterpretation( executionContext, null, listInterpreter );
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return EmptyScrollableResults.INSTANCE;
		}
		return withCacheableSqmInterpretation( executionContext, scrollMode, scrollInterpreter );
	}

	private <T, X> T withCacheableSqmInterpretation(ExecutionContext executionContext, X context, SqmInterpreter<T, X> interpreter) {
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

	private JdbcParameterBindings createJdbcParameterBindings(CacheableSqmInterpretation sqmInterpretation, ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				sqmInterpretation.getJdbcParamsXref(),
				session.getFactory().getDomainModel(),
				sqmInterpretation.getTableGroupAccess()::findTableGroup,
				sqmInterpretation.getSqmParameterMappingModelTypes()::get,
				session
		);
		sqmInterpretation.getJdbcSelect().bindFilterJdbcParameters( jdbcParameterBindings );
		return jdbcParameterBindings;
	}

	private static CacheableSqmInterpretation buildCacheableSqmInterpretation(
			SqmSelectStatement sqm,
			DomainParameterXref domainParameterXref,
			ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final QueryEngine queryEngine = sessionFactory.getQueryEngine();

		final SqmTranslatorFactory sqmTranslatorFactory = queryEngine.getSqmTranslatorFactory();

		final SqmTranslator<SelectStatement> sqmConverter = sqmTranslatorFactory.createSelectTranslator(
				sqm,
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getQueryParameterBindings(),
				executionContext.getLoadQueryInfluencers(),
				sessionFactory
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

		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
				domainParameterXref,
				sqmInterpretation::getJdbcParamsBySqmParam
		);
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				session.getFactory().getDomainModel(),
				tableGroupAccess::findTableGroup,
				sqmInterpretation.getSqmParameterMappingModelTypeResolutions()::get,
				session
		);
		final JdbcSelect jdbcSelect = selectTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		return new CacheableSqmInterpretation(
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
				ExecutionContext executionContext,
				CacheableSqmInterpretation sqmInterpretation,
				JdbcParameterBindings jdbcParameterBindings);
	}

	private static class CacheableSqmInterpretation {
		private final JdbcSelect jdbcSelect;
		private final FromClauseAccess tableGroupAccess;
		private final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> jdbcParamsXref;
		private final Map<SqmParameter, MappingModelExpressable> sqmParameterMappingModelTypes;
		private transient JdbcParameterBindings firstParameterBindings;

		CacheableSqmInterpretation(
				JdbcSelect jdbcSelect,
				FromClauseAccess tableGroupAccess,
				Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> jdbcParamsXref,
				Map<SqmParameter,MappingModelExpressable> sqmParameterMappingModelTypes,
				JdbcParameterBindings firstParameterBindings) {
			this.jdbcSelect = jdbcSelect;
			this.tableGroupAccess = tableGroupAccess;
			this.jdbcParamsXref = jdbcParamsXref;
			this.sqmParameterMappingModelTypes = sqmParameterMappingModelTypes;
			this.firstParameterBindings = firstParameterBindings;
		}

		JdbcSelect getJdbcSelect() {
			return jdbcSelect;
		}

		FromClauseAccess getTableGroupAccess() {
			return tableGroupAccess;
		}

		Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> getJdbcParamsXref() {
			return jdbcParamsXref;
		}

		public Map<SqmParameter, MappingModelExpressable> getSqmParameterMappingModelTypes() {
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
