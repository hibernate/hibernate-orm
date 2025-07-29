/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.internal.util.cache.InternalCache;
import org.hibernate.internal.util.cache.InternalCacheFactory;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.hql.HqlTranslator;
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
import org.hibernate.service.ServiceRegistry;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

/**
 * Standard QueryInterpretationCache implementation
 *
 * @author Steve Ebersole
 */
public class QueryInterpretationCacheStandardImpl implements QueryInterpretationCache {
	private static final Logger log = QueryLogging.subLogger( "plan.cache" );

	/**
	 * the cache of the actual plans...
	 */
	private final InternalCache<Key, QueryPlan> queryPlanCache;

	private final ServiceRegistry serviceRegistry;
	private final InternalCache<Object, HqlInterpretation<?>> hqlInterpretationCache;
	private final InternalCache<String, ParameterInterpretation> nativeQueryParamCache;

	private StatisticsImplementor statistics;

	public QueryInterpretationCacheStandardImpl(int maxQueryPlanCount, ServiceRegistry serviceRegistry) {
		log.tracef( "Starting query interpretation cache (size %s)", maxQueryPlanCount );
		final InternalCacheFactory cacheFactory = serviceRegistry.requireService( InternalCacheFactory.class );
		this.queryPlanCache = cacheFactory.createInternalCache( maxQueryPlanCount );
		this.hqlInterpretationCache = cacheFactory.createInternalCache( maxQueryPlanCount );
		this.nativeQueryParamCache = cacheFactory.createInternalCache( maxQueryPlanCount );
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public int getNumberOfCachedHqlInterpretations() {
		return hqlInterpretationCache.heldElementsEstimate();
	}

	@Override
	public int getNumberOfCachedQueryPlans() {
		return queryPlanCache.heldElementsEstimate();
	}

	private StatisticsImplementor getStatistics() {
		if ( statistics == null ) {
			statistics = serviceRegistry.requireService( StatisticsImplementor.class );
		}
		return statistics;
	}

	@Override
	public <R> SelectQueryPlan<R> resolveSelectQueryPlan(
			Key key,
			Supplier<SelectQueryPlan<R>> creator) {
		return resolveSelectQueryPlan( key, k -> creator.get() );
	}

	@Override
	public <K extends Key, R> SelectQueryPlan<R> resolveSelectQueryPlan(
			K key,
			Function<K, SelectQueryPlan<R>> creator) {
		log.tracef( "Resolving cached query plan for [%s]", key );
		final StatisticsImplementor statistics = getStatistics();
		final boolean stats = statistics.isStatisticsEnabled();

		@SuppressWarnings("unchecked")
		final SelectQueryPlan<R> cached = (SelectQueryPlan<R>) queryPlanCache.get( key );
		if ( cached != null ) {
			if ( stats ) {
				statistics.queryPlanCacheHit( key.getQueryString() );
			}
			return cached;
		}

		final SelectQueryPlan<R> plan = creator.apply( key );
		queryPlanCache.put( key.prepareForStore(), plan );
		if ( stats ) {
			statistics.queryPlanCacheMiss( key.getQueryString() );
		}
		return plan;
	}

	@Override
	public NonSelectQueryPlan getNonSelectQueryPlan(Key key) {
		return null;
	}

	@Override
	public void cacheNonSelectQueryPlan(Key key, NonSelectQueryPlan plan) {
	}

	@Override
	public <R> HqlInterpretation<R> resolveHqlInterpretation(
			String queryString,
			Class<R> expectedResultType,
			HqlTranslator translator) {
		log.tracef( "Resolving HQL interpretation for [%s]", queryString );
		final StatisticsImplementor statistics = getStatistics();

		final Object cacheKey = expectedResultType != null
				? new HqlInterpretationCacheKey( queryString, expectedResultType )
				: queryString;

		final HqlInterpretation<?> existing = hqlInterpretationCache.get( cacheKey );
		if ( existing != null ) {
			if ( statistics.isStatisticsEnabled() ) {
				statistics.queryPlanCacheHit( queryString );
			}
			//noinspection unchecked
			return (HqlInterpretation<R>) existing;
		}
		else if ( expectedResultType != null ) {
			final HqlInterpretation<?> existingQueryOnly = hqlInterpretationCache.get( queryString );
			if ( existingQueryOnly != null ) {
				if ( statistics.isStatisticsEnabled() ) {
					statistics.queryPlanCacheHit( queryString );
				}
				//noinspection unchecked
				return (HqlInterpretation<R>) existingQueryOnly;
			}
		}

		final HqlInterpretation<R> hqlInterpretation =
				createHqlInterpretation( queryString, expectedResultType, translator, statistics );
		hqlInterpretationCache.put( cacheKey, hqlInterpretation );
		return hqlInterpretation;
	}

	@Override
	public <R> void cacheHqlInterpretation(Object cacheKey, HqlInterpretation<R> hqlInterpretation) {
		hqlInterpretationCache.put( cacheKey, hqlInterpretation );
	}

	protected static <R> HqlInterpretation<R> createHqlInterpretation(
			String queryString,
			Class<R> expectedResultType,
			HqlTranslator translator,
			StatisticsImplementor statistics) {
		final boolean stats = statistics.isStatisticsEnabled();
		final long startTime = stats ? System.nanoTime() : 0L;

		final SqmStatement<R> sqmStatement = translator.translate( queryString, expectedResultType );
		final ParameterMetadataImplementor parameterMetadata;
		final DomainParameterXref domainParameterXref;

		if ( sqmStatement.getSqmParameters().isEmpty() ) {
			domainParameterXref = DomainParameterXref.EMPTY;
			parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			domainParameterXref = DomainParameterXref.from( sqmStatement );
			parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		if ( stats ) {
			final long endTime = System.nanoTime();
			final long microseconds = TimeUnit.MICROSECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
			statistics.queryCompiled( queryString, microseconds );
		}

		return new SimpleHqlInterpretationImpl<>( sqmStatement, parameterMetadata, domainParameterXref );
	}

	@Override
	public ParameterInterpretation resolveNativeQueryParameters(
			String queryString,
			Function<String, ParameterInterpretation> creator) {
		log.tracef( "Resolving native query parameters for [%s]", queryString );
		return nativeQueryParamCache.computeIfAbsent( queryString, creator );
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void close() {
		log.trace( "Destroying query interpretation cache" );
		hqlInterpretationCache.clear();
		nativeQueryParamCache.clear();
		queryPlanCache.clear();
	}

	/**
	 * Interpretation-cache key used for HQL interpretations
	 */
	private record HqlInterpretationCacheKey(String queryString, Class<?> expectedResultType) {
	}

}
