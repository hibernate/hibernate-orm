/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * @author Steve Ebersole
 */
public class QueryInterpretationCacheDisabledImpl implements QueryInterpretationCache {
	/**
	 * Singleton access
	 */
	public static final QueryInterpretationCacheDisabledImpl INSTANCE = new QueryInterpretationCacheDisabledImpl();

	@Override
	public int getNumberOfCachedHqlInterpretations() {
		return 0;
	}

	@Override
	public int getNumberOfCachedQueryPlans() {
		return 0;
	}

	@Override
	public SelectQueryPlan resolveSelectQueryPlan(Key key, Supplier<SelectQueryPlan> creator) {
		return null;
	}

	@Override
	public NonSelectQueryPlan getNonSelectQueryPlan(Key key) {
		return null;
	}

	@Override
	public void cacheNonSelectQueryPlan(Key key, NonSelectQueryPlan plan) {
	}

	@Override
	public HqlInterpretation resolveHqlInterpretation(String queryString, Function<String, SqmStatement<?>> creator) {
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

		return new HqlInterpretation() {
			@Override
			public SqmStatement getSqmStatement() {
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
