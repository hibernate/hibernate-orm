/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.concurrent.TimeUnit;
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
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Steve Ebersole
 */
public class QueryInterpretationCacheDisabledImpl implements QueryInterpretationCache {

	private final Supplier<StatisticsImplementor> statisticsSupplier;

	public QueryInterpretationCacheDisabledImpl(Supplier<StatisticsImplementor> statisticsSupplier) {
		this.statisticsSupplier = statisticsSupplier;
	}

	@Override
	public int getNumberOfCachedHqlInterpretations() {
		return 0;
	}

	@Override
	public int getNumberOfCachedQueryPlans() {
		return 0;
	}

	@Override
	public <R> SelectQueryPlan<R> resolveSelectQueryPlan(Key key, Supplier<SelectQueryPlan<R>> creator) {
		final StatisticsImplementor statistics = statisticsSupplier.get();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.queryPlanCacheMiss( key.getQueryString() );
		}
		return creator.get();
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
		final StatisticsImplementor statistics = statisticsSupplier.get();
		final boolean stats = statistics.isStatisticsEnabled();
		final long startTime = stats ? System.nanoTime() : 0L;

		final SqmStatement<R> sqmStatement = translator.translate( queryString, expectedResultType );

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

		if ( stats ) {
			final long endTime = System.nanoTime();
			final long microseconds = TimeUnit.MICROSECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
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
		};
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
