/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sqm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.sql.sqm.exec.spi.QueryOptions;

/**
 * @author Steve Ebersole
 */
public class AggregatedSelectQueryPlanImpl<R> implements SelectQueryPlan<R> {
	private final SelectQueryPlan<R>[] aggregatedQueryPlans;

	public AggregatedSelectQueryPlanImpl(SelectQueryPlan<R>[] aggregatedQueryPlans) {
		this.aggregatedQueryPlans = aggregatedQueryPlans;
	}

	@Override
	public List<R> performList(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		final List<R> overallResults = new ArrayList<R>();

		for ( SelectQueryPlan<R> aggregatedQueryPlan : aggregatedQueryPlans ) {
			overallResults.addAll(
					aggregatedQueryPlan.performList( persistenceContext, queryOptions, inputParameterBindings )
			);
		}

		return overallResults;
	}

	@Override
	public Iterator<R> performIterate(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		return null;
	}

	@Override
	public ScrollableResultsImplementor performScroll(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings,
			ScrollMode scrollMode) {
		return null;
	}
}
