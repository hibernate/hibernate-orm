/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.stat.spi.StatisticsImplementor;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @author Steve Ebersole
 */
public class QueryInterpretationCacheDisabledImpl implements QueryInterpretationCache {

	private final ServiceRegistry serviceRegistry;

	private StatisticsImplementor statistics;

	public QueryInterpretationCacheDisabledImpl(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public int getNumberOfCachedHqlInterpretations() {
		return 0;
	}

	@Override
	public int getNumberOfCachedQueryPlans() {
		return 0;
	}

	private StatisticsImplementor getStatistics() {
		if ( statistics == null ) {
			statistics = serviceRegistry.requireService( StatisticsImplementor.class );
		}
		return statistics;
	}

	@Override
	public <R> SelectQueryPlan<R> resolveSelectQueryPlan(Key key, Supplier<SelectQueryPlan<R>> creator) {
		final var statistics = getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.queryPlanCacheMiss( key.getQueryString() );
		}
		return creator.get();
	}

	@Override
	public <K extends Key, R> SelectQueryPlan<R> resolveSelectQueryPlan(K key, Function<K, SelectQueryPlan<R>> creator) {
		final var statistics = getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.queryPlanCacheMiss( key.getQueryString() );
		}
		return creator.apply( key );
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
			String queryString, Class<R> expectedResultType, HqlTranslator translator) {
		final var statistics = getStatistics();
		final boolean statisticsEnabled = statistics.isStatisticsEnabled();
		final long startTime = statisticsEnabled ? System.nanoTime() : 0L;

		final var sqmStatement = translator.translate( queryString, expectedResultType );

		final boolean hasParameters = sqmStatement.getSqmParameters().isEmpty();
		final var domainParameterXref =
				hasParameters
						? DomainParameterXref.EMPTY
						: DomainParameterXref.from( sqmStatement );
		final var parameterMetadata =
				hasParameters
						? ParameterMetadataImpl.EMPTY
						: new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		if ( statisticsEnabled ) {
			final long endTime = System.nanoTime();
			final long microseconds = MICROSECONDS.convert( endTime - startTime, NANOSECONDS );
			statistics.queryCompiled( queryString, microseconds );
		}

		return new HqlInterpretation<>() {
			@Override
			public SqmStatement<R> getSqmStatement() {
				return sqmStatement;
			}

			@Override
			public ParameterMetadataImplementor getParameterMetadata() {
				return parameterMetadata;
			}

			@Override
			public DomainParameterXref getDomainParameterXref() {
				return domainParameterXref;
			}

			@Override
			public void validateResultType(Class<?> resultType) {
				( (SqmSelectStatement<R>) sqmStatement ).validateResultType( resultType );
			}
		};
	}

	@Override
	public <R> void cacheHqlInterpretation(Object cacheKey, HqlInterpretation<R> hqlInterpretation) {
		// nothing to do
	}

	@Override
	public ParameterInterpretation resolveNativeQueryParameters(
			String queryString,
			Function<String, ParameterInterpretation> creator) {
		return creator.apply( queryString );
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public void close() {
	}
}
