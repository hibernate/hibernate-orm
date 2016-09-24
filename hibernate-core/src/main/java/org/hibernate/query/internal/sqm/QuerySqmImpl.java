/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sqm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractQuery;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryInterpretations;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.spi.SqmBackedQuery;
import org.hibernate.sql.sqm.exec.internal.QueryOptionsImpl;
import org.hibernate.sql.sqm.exec.spi.QueryOptions;
import org.hibernate.sqm.QuerySplitter;
import org.hibernate.sqm.query.SqmSelectStatement;
import org.hibernate.sqm.query.SqmStatement;
import org.hibernate.sqm.query.SqmStatementNonSelect;

import org.hibernate.query.internal.ParameterMetadataImpl;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R> extends AbstractQuery<R> implements SqmBackedQuery<R> {
	private final String sourceQueryString;
	private final SqmStatement sqmStatement;
	private final Class resultType;

	private final ParameterMetadataImpl parameterMetadata;
	private final QueryParameterBindingsImpl parameterBindings;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	private EntityGraphQueryHint entityGraphQueryHint;

	public QuerySqmImpl(
			String sourceQueryString,
			SqmStatement sqmStatement,
			Class resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		if ( resultType != null ) {
			if ( sqmStatement instanceof SqmStatementNonSelect ) {
				throw new IllegalArgumentException( "Non-select queries cannot be typed" );
			}
		}

		this.sourceQueryString = sourceQueryString;
		this.sqmStatement = sqmStatement;
		this.resultType = resultType;

		this.parameterMetadata = buildParameterMetadata( sqmStatement );
		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );
	}

	private static ParameterMetadataImpl buildParameterMetadata(SqmStatement sqm) {
		Map<String, QueryParameter> namedQueryParameters = null;
		Map<Integer, QueryParameter> positionalQueryParameters = null;

		for ( org.hibernate.sqm.query.Parameter parameter : sqm.getQueryParameters() ) {
			if ( parameter.getName() != null ) {
				if ( namedQueryParameters == null ) {
					namedQueryParameters = new HashMap<>();
				}
				namedQueryParameters.put(
						parameter.getName(),
						QueryParameterNamedImpl.fromSqm( parameter )
				);
			}
			else if ( parameter.getPosition() != null ) {
				if ( positionalQueryParameters == null ) {
					positionalQueryParameters = new HashMap<>();
				}
				positionalQueryParameters.put(
						parameter.getPosition(),
						QueryParameterPositionalImpl.fromSqm( parameter )
				);
			}
		}

		return new ParameterMetadataImpl( namedQueryParameters, positionalQueryParameters );
	}

	@Override
	protected boolean canApplyAliasSpecificLockModes() {
		return isSelect();
	}

	@Override
	protected void verifySettingLockMode() {
		if ( !isSelect() ) {
			throw new IllegalStateException( "Illegal attempt to set lock mode on a non-SELECT query" );
		}
	}

	private boolean isSelect() {
		return sqmStatement instanceof SqmSelectStatement;
	}

	@Override
	protected void verifySettingAliasSpecificLockModes() {
		// todo : add a specific Dialect check as well? - if not supported, maybe that can trigger follow-on locks?
		verifySettingLockMode();
	}

	@Override
	public String getQueryString() {
		return sourceQueryString;
	}

	@Override
	public SqmStatement getSqmStatement() {
		return sqmStatement;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		return parameterMetadata.collectAllParametersJpa();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		try {
			return parameterMetadata.getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getProducer().getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		try {
			final QueryParameter parameter = parameterMetadata.getQueryParameter( name );
			if ( !parameter.getParameterType().isAssignableFrom( type ) ) {
				throw new IllegalArgumentException(
						"The type [" + parameter.getParameterType().getName() +
								"] associated with the parameter corresponding to name [" + name +
								"] is not assignable to requested Java type [" + type.getName() + "]"
				);
			}
			return parameter;
		}
		catch ( HibernateException e ) {
			throw getProducer().getExceptionConverter().convert( e );
		}
	}

	@Override
	public Parameter<?> getParameter(int position) {
		try {
			return parameterMetadata.getQueryParameter( position );
		}
		catch ( HibernateException e ) {
			throw getProducer().getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		try {
			final QueryParameter parameter = parameterMetadata.getQueryParameter( position );
			if ( !parameter.getParameterType().isAssignableFrom( type ) ) {
				throw new IllegalArgumentException(
						"The type [" + parameter.getParameterType().getName() +
								"] associated with the parameter corresponding to position [" + position +
								"] is not assignable to requested Java type [" + type.getName() + "]"
				);
			}
			return parameter;
		}
		catch ( HibernateException e ) {
			throw getProducer().getExceptionConverter().convert( e );
		}
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		final QueryParameter qp = parameterMetadata.resolve( param );
		return qp != null && parameterBindings.isBound( qp );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T> param) {
		final QueryParameter qp = parameterMetadata.resolve( param );
		if ( qp == null ) {
			throw new IllegalArgumentException( "The parameter [" + param + "] is not part of this Query" );
		}

		final QueryParameterBinding binding = parameterBindings.getBinding( qp );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + param + "] has not yet been bound" );
		}

		if ( binding.isMultiValued() ) {
			return (T) binding.getBindValues();
		}
		else {
			return (T) binding.getBindValue();
		}
	}

	@Override
	public Object getParameterValue(String name) {
		final QueryParameter qp = parameterMetadata.getQueryParameter( name );
		if ( qp == null ) {
			throw new IllegalArgumentException( "The parameter [" + name + "] is not part of this Query" );
		}

		final QueryParameterBinding binding = parameterBindings.getBinding( qp );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + name + "] has not yet been bound" );
		}

		if ( binding.isMultiValued() ) {
			return binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	@Override
	public Object getParameterValue(int position) {
		final QueryParameter qp = parameterMetadata.getQueryParameter( position );
		if ( qp == null ) {
			throw new IllegalArgumentException( "The parameter [" + position + "] is not part of this Query" );
		}

		final QueryParameterBinding binding = parameterBindings.getBinding( qp );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + position + "] has not yet been bound" );
		}

		if ( binding.isMultiValued() ) {
			return binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public EntityGraphQueryHint getEntityGraphHint() {
		return entityGraphQueryHint;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected QueryParameterBinding locateBinding(QueryParameter parameter) {
		return parameterBindings.getBinding( parameter );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected QueryParameterBinding locateBinding(String name) {
		return parameterBindings.getBinding( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected QueryParameterBinding locateBinding(int position) {
		return parameterBindings.getBinding( position );
	}

	@Override
	protected void verifyParameterBindings() {
		parameterBindings.validate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		if ( cls.isInstance( parameterMetadata ) ) {
			return (T) parameterMetadata;
		}

		if ( cls.isInstance( parameterBindings ) ) {
			return (T) parameterBindings;
		}

		if ( cls.isInstance( sqmStatement ) ) {
			return (T) sqmStatement;
		}

		if ( cls.isInstance( queryOptions ) ) {
			return (T) queryOptions;
		}

		if ( cls.isInstance( entityGraphQueryHint ) ) {
			return (T) entityGraphQueryHint;
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
	}

	protected boolean applyNativeQueryLockMode(Object value) {
		throw new IllegalStateException(
				"Illegal attempt to set lock mode on non-native query via hint; use Query#setLockMode instead"
		);
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, EntityGraphImpl entityGraph) {
		this.entityGraphQueryHint = new EntityGraphQueryHint( hintName, entityGraph );
	}


	@Override
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( entityGraphQueryHint != null ) {
			hints.put( entityGraphQueryHint.getHintName(), entityGraphQueryHint.getOriginEntityGraph() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<R> doList() {
		verifyTransactionInProgressIfRequired();

		// todo:
		//		1) build SelectQueryPlan
		//		2) getProducer().performList( builtSelectQueryPlan )
		//
		// this (^^) approach can be reused across SQM and native
		//
		// Producer#performList ->
		//		1) checkOpen();
		//		2) checkTransactionSynchStatus();
		//		3) SelectQueryPlan#performList

		// ofc, as an alt we could just do away with the callout to
		// getProducer().performList( builtSelectQueryPlan ) and instead do:
		//		1) build SelectQueryPlan
		//		2) getProducer().checkOpen();
		//		3) getProducer().checkTransactionSynchStatus();
		//		4) builtSelectQueryPlan#performList

		getProducer().checkOpen();
		getProducer().checkTransactionSynchStatus();

		return getProducer().performList( this );

//		return getProducer().list(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}


	@SuppressWarnings("unchecked")
	protected List<R> doList2() {
		// should coordinate #checkOpen and #checkTransactionSynchStatus
		getProducer().prepareForQueryExecution();
		verifyTransactionInProgressIfRequired();

		return resolveSelectQueryPlan( this ).performList(
				getProducer(),
				getQueryOptions(),
				getQueryParameterBindings()
		);
	}

	@SuppressWarnings("unchecked")
	private <R> SelectQueryPlan<R> resolveSelectQueryPlan(SqmBackedQuery<R> query) {
		// resolve (or make) the QueryPlan.  This QueryPlan might be an
		// aggregation of multiple plans.  QueryPlans can be cached, except
		// for in certain circumstances, the determination of which occurs in
		// SqmInterpretationsKey#generateFrom - if SqmInterpretationsKey#generateFrom
		// returns null the query is not cacheable

		SelectQueryPlan<R> queryPlan = null;

		final QueryInterpretations.Key cacheKey = SqmInterpretationsKey.generateFrom( query );
		if ( cacheKey != null ) {
			queryPlan = getProducer().getFactory().getQueryInterpretations().getSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = buildSelectQueryPlan( query );
			if ( cacheKey != null ) {
				getProducer().getFactory().getQueryInterpretations().cacheSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	private <R> SelectQueryPlan<R> buildSelectQueryPlan(SqmBackedQuery<R> query) {
		final SqmSelectStatement[] concreteSqmStatements = QuerySplitter.split( (SqmSelectStatement) query.getSqmStatement() );
		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedSelectQueryPlan( query, concreteSqmStatements );
		}
		else {
			return buildSimpleSelectQueryPlan(
					concreteSqmStatements[0],
					query.getResultType(),
					query.getEntityGraphHint(),
					query.getQueryOptions()
			);
		}
	}

	@SuppressWarnings("unchecked")
	private <R> SelectQueryPlan<R> buildAggregatedSelectQueryPlan(
			SqmBackedQuery<R> query,
			SqmSelectStatement[] concreteSqmStatements) {
		final SelectQueryPlan[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];

		// todo : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildSimpleSelectQueryPlan(
					concreteSqmStatements[i],
					query.getResultType(),
					query.getEntityGraphHint(),
					query.getQueryOptions()
			);
		}

		return new AggregatedSelectQueryPlanImpl( aggregatedQueryPlans );
	}

	private <R> SelectQueryPlan<R> buildSimpleSelectQueryPlan(
			SqmSelectStatement concreteSqmStatement,
			Class<R> resultType,
			EntityGraphQueryHint entityGraphHint,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				entityGraphHint,
				resultType,
				queryOptions
		);
	}

	private void verifyTransactionInProgressIfRequired() {
		final LockMode lockMode = getLockOptions().findGreatestLockMode();
		if ( lockMode != null && lockMode.greaterThan( LockMode.NONE ) ) {
			if ( !getProducer().isTransactionInProgress() ) {
				throw new TransactionRequiredException(
						"Locking [" + lockMode.name() + "] was requested on Query, but no transaction is in progress"
				);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Iterator<R> doIterate() {
		verifyTransactionInProgressIfRequired();

		return getProducer().performIterate( this );
//		return getProducer().iterate(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}

	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		verifyTransactionInProgressIfRequired();

		return getProducer().performScroll( this, scrollMode );
//		return getProducer().scroll(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}

	@Override
	protected int doExecuteUpdate() {
		return getProducer().executeUpdate( this );
//		return getProducer().executeUpdate(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}
}
