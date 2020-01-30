/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.ScrollMode;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.streams.StingArrayCollector;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.sql.SqmSelectTranslation;
import org.hibernate.query.sqm.sql.SqmSelectTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerJpaTupleImpl;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.results.internal.TupleElementImpl;
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

	@SuppressWarnings("WeakerAccess")
	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement sqm,
			DomainParameterXref domainParameterXref,
			Class<R> resultType,
			QueryOptions queryOptions) {
		this.sqm = sqm;
		this.domainParameterXref = domainParameterXref;

		this.rowTransformer = determineRowTransformer( sqm, resultType, queryOptions );

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

		if ( Tuple.class.isAssignableFrom( resultType ) ) {
			// resultType is Tuple..
			if ( queryOptions.getTupleTransformer() == null ) {
				final List<TupleElement<?>> tupleElementList = new ArrayList<>();
				for ( SqmSelection selection : sqm.getQuerySpec().getSelectClause().getSelections() ) {
					tupleElementList.add(
							new TupleElementImpl(
									selection.getSelectableNode().getJavaTypeDescriptor().getJavaType(),
									selection.getAlias()
							)
					);
				}
				return (RowTransformer<R>) new RowTransformerJpaTupleImpl( tupleElementList );
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
		else if ( sqm.getQuerySpec().getSelectClause().getSelections().size() > 1 ) {
			throw new IllegalQueryOperationException( "Query defined multiple selections, return cannot be typed (other that Object[] or Tuple)" );
		}
		else {
			return RowTransformerSingularReturnImpl.instance();
		}
	}

	@SuppressWarnings("unchecked")
	private RowTransformer makeRowTransformerTupleTransformerAdapter(
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
		final SharedSessionContractImplementor session = executionContext.getSession();

		final CacheableSqmInterpretation sqmInterpretation = resolveCacheableSqmInterpretation( executionContext );

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				sqmInterpretation.getJdbcParamsXref(),
				session.getFactory().getDomainModel(),
				sqmInterpretation.getTableGroupAccess()::findTableGroup,
				session
		);

		try {
			return session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
					sqmInterpretation.getJdbcSelect(),
					jdbcParameterBindings,
					executionContext,
					rowTransformer,
					true
			);
		}
		finally {
			domainParameterXref.clearExpansions();
		}
	}

	private volatile CacheableSqmInterpretation cacheableSqmInterpretation;

	private CacheableSqmInterpretation resolveCacheableSqmInterpretation(ExecutionContext executionContext) {
		// NOTE : VERY IMPORTANT - intentional double-lock checking
		//		The other option would be to leverage `java.util.concurrent.locks.ReadWriteLock`
		//		to protect access.  However, synchronized is much simpler here.  We will verify
		// 		during throughput testing whether this is an issue and consider changes then

		CacheableSqmInterpretation localCopy = cacheableSqmInterpretation;

		if ( localCopy == null ) {
			synchronized ( this ) {
				localCopy = cacheableSqmInterpretation;
				if ( localCopy == null ) {
					localCopy = buildCacheableSqmInterpretation(
							sqm,
							domainParameterXref,
							executionContext
					);
					cacheableSqmInterpretation = localCopy;
				}
			}
		}

		return localCopy;
	}

	private static CacheableSqmInterpretation buildCacheableSqmInterpretation(
			SqmSelectStatement sqm,
			DomainParameterXref domainParameterXref, ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final QueryEngine queryEngine = sessionFactory.getQueryEngine();

		final SqmTranslatorFactory sqmTranslatorFactory = queryEngine.getSqmTranslatorFactory();

		final SqmSelectTranslator sqmConverter = sqmTranslatorFactory.createSelectTranslator(
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getQueryParameterBindings(),
				executionContext.getLoadQueryInfluencers(),
				sessionFactory
		);

//			tableGroupAccess = sqmConverter.getFromClauseAccess();
		final FromClauseAccess tableGroupAccess = sqmConverter.getFromClauseAccess();

		final SqmSelectTranslation interpretation = sqmConverter.translate( sqm );

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( interpretation.getSqlAst() );

		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
				domainParameterXref,
				interpretation::getJdbcParamsBySqmParam
		);

		return new CacheableSqmInterpretation( jdbcSelect, tableGroupAccess, jdbcParamsXref );
	}

	private static class CacheableSqmInterpretation {
		private final JdbcSelect jdbcSelect;
		private final FromClauseAccess tableGroupAccess;
		private final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref;

		CacheableSqmInterpretation(
				JdbcSelect jdbcSelect,
				FromClauseAccess tableGroupAccess,
				Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref) {
			this.jdbcSelect = jdbcSelect;
			this.tableGroupAccess = tableGroupAccess;
			this.jdbcParamsXref = jdbcParamsXref;
		}

		JdbcSelect getJdbcSelect() {
			return jdbcSelect;
		}

		FromClauseAccess getTableGroupAccess() {
			return tableGroupAccess;
		}

		Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> getJdbcParamsXref() {
			return jdbcParamsXref;
		}
	}





	@Override
	@SuppressWarnings("unchecked")
	public ScrollableResultsImplementor performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception( getClass() );

//		final SqmSelectToSqlAstConverter sqmConverter = getSqmSelectToSqlAstConverter( executionContext );
//
//		final SqmSelectInterpretation interpretation = sqmConverter.interpret( sqm );
//
//		final JdbcSelect jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
//				interpretation,
//				executionContext.getSession().getSessionFactory()
//		);
//
//		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref =
//				SqmConsumeHelper.generateJdbcParamsXref( domainParameterXref, sqmConverter );
//
//		final JdbcParameterBindings jdbcParameterBindings = QueryHelper.createJdbcParameterBindings(
//				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
//				domainParameterXref,
//				jdbcParamsXref,
//				executionContext.getSession()
//		);
//
//		try {
//			return JdbcSelectExecutorStandardImpl.INSTANCE.scroll(
//					jdbcSelect,
//					scrollMode,
//					jdbcParameterBindings,
//					executionContext,
//					rowTransformer
//			);
//		}
//		finally {
//			domainParameterXref.clearExpansions();
//		}
	}
}
