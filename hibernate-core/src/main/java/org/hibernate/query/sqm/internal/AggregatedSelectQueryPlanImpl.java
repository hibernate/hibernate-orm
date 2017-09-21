/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class AggregatedSelectQueryPlanImpl<R> implements SelectQueryPlan<R> {
	private final SelectQueryPlan<R>[] aggregatedQueryPlans;

	public AggregatedSelectQueryPlanImpl(SelectQueryPlan<R>[] aggregatedQueryPlans) {
		this.aggregatedQueryPlans = aggregatedQueryPlans;
	}

	@Override
	public List<R> performList(ExecutionContext executionContext) {
		final List<R> overallResults = new ArrayList<R>();

		for ( SelectQueryPlan<R> aggregatedQueryPlan : aggregatedQueryPlans ) {
			overallResults.addAll( aggregatedQueryPlan.performList( executionContext ) );
		}

		return overallResults;
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception();
	}
}
