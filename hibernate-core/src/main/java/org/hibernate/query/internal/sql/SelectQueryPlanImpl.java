/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import java.util.Iterator;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class SelectQueryPlanImpl<R> implements SelectQueryPlan<R> {
	private final String sql;
	private final boolean callable;
	private final boolean autoDiscoverTypes;

	private final List<JdbcParameterBinder> parameterBinders;

	private final RowTransformer<R> rowTransformer;

	public SelectQueryPlanImpl(NativeQueryImpl<R> nativeQuery) {
		this.sql = nativeQuery.getQueryString();
		this.callable = nativeQuery.isCallable();
		this.autoDiscoverTypes = nativeQuery.isAutoDiscoverTypes();

		this.parameterBinders = nativeQuery.getParameterBinders();

		this.rowTransformer = determineRowTransformer( nativeQuery );
	}

	private RowTransformer<R> determineRowTransformer(NativeQueryImpl<R> nativeQuery) {
		if ( nativeQuery.getQueryReturns() != null && !nativeQuery.getQueryReturns().isEmpty() ) {

		}
		return null;
	}

	@Override
	public List<R> performList(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		throw new NotYetImplementedException( "Not yet implemented" );
	}

	@Override
	public Iterator<R> performIterate(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		throw new NotYetImplementedException( "Not yet implemented" );
	}

	@Override
	public ScrollableResultsImplementor performScroll(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings,
			ScrollMode scrollMode) {
		throw new NotYetImplementedException( "Not yet implemented" );
	}
}
