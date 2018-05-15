/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.streams.StingArrayCollector;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.JpaTupleTransformer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.exec.internal.Helper;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerJpaTupleImpl;
import org.hibernate.sql.exec.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.exec.internal.TupleElementImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * Standard Hibernate implementation of SelectQueryPlan for SQM-backed
 * {@link org.hibernate.query.Query} implementations, which means
 * HQL/JPQL or {@link javax.persistence.criteria.CriteriaQuery}
 *
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement sqm;
	private final Map<QueryParameterImplementor, SqmParameter> sqmParamByQueryParam;
	private final RowTransformer<R> rowTransformer;

	private JdbcSelect jdbcSelect;
	private  Map<QueryParameterImplementor, List<JdbcParameter>> jdbcParamsByDomainParams;

	@SuppressWarnings("WeakerAccess")
	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement sqm,
			Map<QueryParameterImplementor, SqmParameter> sqmParamByQueryParam,
			Class<R> resultType,
			QueryOptions queryOptions) {
		this.sqm = sqm;
		this.sqmParamByQueryParam = sqmParamByQueryParam;

		this.rowTransformer = determineRowTransformer( sqm, resultType, queryOptions );
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
//				return (RowTransformer<R>) new RowTransformerTupleImpl(
//						sqm.getQuerySpec().getSelectClause().getSelections()
//								.stream()
//								.map( selection -> (TupleElement<?>) new TupleElementImpl(
//										( (SqmTypeImplementor) selection.asExpression().getExpressableType() ).getDomainType().getReturnedClass(),
//										selection.getAlias()
//								) )
//								.collect( Collectors.toList() )
//				);
			}

			// there can be a TupleTransformer IF it is a JpaTupleBuilder,
			// otherwise this is considered an error
			if ( queryOptions.getTupleTransformer() instanceof JpaTupleTransformer ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
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
	@SuppressWarnings("unchecked")
	public List<R> performList(ExecutionContext executionContext) {
		if ( jdbcSelect == null ) {
			SqmSelectToSqlAstConverter sqmConverter = getSqmSelectToSqlAstConverter( executionContext );
			SqlAstSelectDescriptor interpretation = sqmConverter.interpret( sqm );
			jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
					interpretation,
					executionContext.getSession().getSessionFactory()
			);

			jdbcParamsByDomainParams = generateJdbcParamsByQueryParamMap(
					sqmConverter );
		}

		final JdbcParameterBindings jdbcParameterBindings = Helper.createJdbcParameterBindings(
				executionContext.getParameterBindingContext().getQueryParameterBindings(),
				jdbcParamsByDomainParams,
				executionContext.getSession()
		);

		final ExecutionContext realExecutionContext = new ExecutionContext() {
			@Override
			public SharedSessionContractImplementor getSession() {
				return executionContext.getSession();
			}

			@Override
			public QueryOptions getQueryOptions() {
				return executionContext.getQueryOptions();
			}

			@Override
			public ParameterBindingContext getParameterBindingContext() {
				return executionContext.getParameterBindingContext();
			}

			@Override
			public Callback getCallback() {
				return executionContext.getCallback();
			}

			@Override
			public JdbcParameterBindings getJdbcParameterBindings() {
				return jdbcParameterBindings;
			}
		};

		// todo (6.0) : make these executors resolvable to allow plugging in custom ones.
		//		Dialect?
		return JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				realExecutionContext,
				rowTransformer
		);
	}

	private Map<QueryParameterImplementor, List<JdbcParameter>> generateJdbcParamsByQueryParamMap(
			SqmSelectToSqlAstConverter sqmConverter) {
		final Map<QueryParameterImplementor,List<JdbcParameter>> jdbcParamsByDomainParams = new HashMap<>();

		for ( Map.Entry<QueryParameterImplementor, SqmParameter> paramEntry : sqmParamByQueryParam.entrySet() ) {
			final List<JdbcParameter> jdbcParameters = sqmConverter.getJdbcParamsBySqmParam().get( paramEntry.getValue() );
			jdbcParamsByDomainParams.put( paramEntry.getKey(), jdbcParameters );
		}
		return jdbcParamsByDomainParams;
	}

	private SqmSelectToSqlAstConverter getSqmSelectToSqlAstConverter(ExecutionContext executionContext) {
		// todo (6.0) : for cases where we have no "load query influencers" we could use a cached SQL AST
		return new SqmSelectToSqlAstConverter(
					executionContext.getQueryOptions(),
					new SqlAstProducerContext() {
						@Override
						public SessionFactoryImplementor getSessionFactory() {
							return executionContext.getSession().getFactory();
						}

						@Override
						public LoadQueryInfluencers getLoadQueryInfluencers() {
							return executionContext.getSession().getLoadQueryInfluencers();
						}

						@Override
						public Callback getCallback() {
							return executionContext.getCallback();
						}
					}
			);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ScrollableResultsImplementor performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {

		final SqmSelectToSqlAstConverter sqmConverter = getSqmSelectToSqlAstConverter( executionContext );

		final SqlAstSelectDescriptor interpretation = sqmConverter.interpret( sqm );

		final JdbcSelect jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
				interpretation,
				executionContext.getSession().getSessionFactory()
		);

		final Map<QueryParameterImplementor, List<JdbcParameter>> jdbcParamsByDomainParams
				= generateJdbcParamsByQueryParamMap( sqmConverter );

		final JdbcParameterBindings jdbcParameterBindings = Helper.createJdbcParameterBindings(
				executionContext.getParameterBindingContext().getQueryParameterBindings(),
				jdbcParamsByDomainParams,
				executionContext.getSession()
		);

		final ExecutionContext realExecutionContext = new ExecutionContext() {
			@Override
			public SharedSessionContractImplementor getSession() {
				return executionContext.getSession();
			}

			@Override
			public QueryOptions getQueryOptions() {
				return executionContext.getQueryOptions();
			}

			@Override
			public ParameterBindingContext getParameterBindingContext() {
				return executionContext.getParameterBindingContext();
			}

			@Override
			public Callback getCallback() {
				return executionContext.getCallback();
			}

			@Override
			public JdbcParameterBindings getJdbcParameterBindings() {
				return jdbcParameterBindings;
			}
		};

		return JdbcSelectExecutorStandardImpl.INSTANCE.scroll(
				jdbcSelect,
				scrollMode,
				realExecutionContext,
				rowTransformer
		);
	}
}
