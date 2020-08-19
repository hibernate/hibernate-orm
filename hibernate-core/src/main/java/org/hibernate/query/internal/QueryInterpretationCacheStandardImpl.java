/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryPlan;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.spi.SimpleHqlInterpretationImpl;
import org.hibernate.query.sql.spi.ParameterInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;

import org.jboss.logging.Logger;

/**
 * Standard QueryPlanCache implementation
 *
 * @author Steve Ebersole
 */
public class QueryInterpretationCacheStandardImpl implements QueryInterpretationCache {
	private static final Logger log = QueryLogging.subLogger( "plan.cache" );

	/**
	 * The default strong reference count.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated
	public static final int DEFAULT_PARAMETER_METADATA_MAX_COUNT = 128;

	/**
	 * The default soft reference count.
	 */
	public static final int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;

	/**
	 * the cache of the actual plans...
	 */
	private final BoundedConcurrentHashMap<Key, QueryPlan> queryPlanCache;

	private final BoundedConcurrentHashMap<String, HqlInterpretation> hqlInterpretationCache;
	private final BoundedConcurrentHashMap<String, ParameterInterpretation> nativeQueryParamCache;

	public QueryInterpretationCacheStandardImpl(int maxQueryPlanCount) {
		log.debugf( "Starting QueryPlanCache(%s)", maxQueryPlanCount );

		queryPlanCache = new BoundedConcurrentHashMap<>( maxQueryPlanCount, 20, BoundedConcurrentHashMap.Eviction.LIRS );
		hqlInterpretationCache = new BoundedConcurrentHashMap<>( maxQueryPlanCount, 20, BoundedConcurrentHashMap.Eviction.LIRS );
		nativeQueryParamCache = new BoundedConcurrentHashMap<>( maxQueryPlanCount, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	@Override
	public int getNumberOfCachedHqlInterpretations() {
		return hqlInterpretationCache.size();
	}

	@Override
	public int getNumberOfCachedQueryPlans() {
		return queryPlanCache.size();
	}

	@Override
	public SelectQueryPlan resolveSelectQueryPlan(
			Key key,
			Supplier<SelectQueryPlan> creator) {
		log.tracef( "QueryPlan#getSelectQueryPlan(%s)", key );

		final SelectQueryPlan cached = (SelectQueryPlan) queryPlanCache.get( key );
		if ( cached != null ) {
			return cached;
		}

		final SelectQueryPlan plan = creator.get();
		queryPlanCache.put( key, plan );
		return plan;
	}

	@Override
	public NonSelectQueryPlan getNonSelectQueryPlan(Key key) {
		log.tracef( "QueryPlan#getNonSelectQueryPlan(%s)", key );
		return null;
	}

	@Override
	public void cacheNonSelectQueryPlan(Key key, NonSelectQueryPlan plan) {
		log.tracef( "QueryPlan#cacheNonSelectQueryPlan(%s)", key );
	}

	@Override
	public HqlInterpretation resolveHqlInterpretation(
			String queryString,
			Function<String, SqmStatement<?>> creator) {
		log.tracef( "QueryPlan#resolveHqlInterpretation( `%s` )", queryString );

		final HqlInterpretation cached = hqlInterpretationCache.get( queryString );
		if ( cached != null ) {
			return cached;
		}

		log.debugf( "Creating and caching HqlInterpretation - %s", queryString );

		final SqmStatement<?> sqmStatement = creator.apply( queryString );
		final DomainParameterXref domainParameterXref;
		final ParameterMetadataImplementor parameterMetadata;

		if ( sqmStatement.getSqmParameters().isEmpty() ) {
			domainParameterXref = DomainParameterXref.empty();
			parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			domainParameterXref = DomainParameterXref.from( sqmStatement );
			parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		final HqlInterpretation interpretation = new SimpleHqlInterpretationImpl(
				sqmStatement,
				parameterMetadata,
				domainParameterXref);
		hqlInterpretationCache.put( queryString, interpretation );
		return interpretation;
	}

	@Override
	public ParameterInterpretation resolveNativeQueryParameters(
			String queryString,
			Function<String, ParameterInterpretation> creator) {
		log.tracef( "QueryPlan#resolveNativeQueryParameters(%s)", queryString );
		return nativeQueryParamCache.computeIfAbsent(
				queryString,
				s -> {
					final ParameterInterpretation interpretation = creator.apply( queryString );
					log.debugf( "Creating and caching NativeQuery ParameterInterpretation - %s", interpretation );
					return interpretation;
				}
		);
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void close() {
		// todo (6.0) : clear maps/caches and LOG
		queryPlanCache.clear();
	}
}
