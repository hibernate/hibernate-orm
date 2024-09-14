/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;

/**
 * @author Christian Beikov
 */
public class AggregatedNonSelectQueryPlanImpl implements NonSelectQueryPlan {
	private final NonSelectQueryPlan[] aggregatedQueryPlans;

	public AggregatedNonSelectQueryPlanImpl(NonSelectQueryPlan[] aggregatedQueryPlans) {
		this.aggregatedQueryPlans = aggregatedQueryPlans;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		int updated = 0;
		for ( NonSelectQueryPlan aggregatedQueryPlan : aggregatedQueryPlans ) {
			updated += aggregatedQueryPlan.executeUpdate( executionContext );
		}
		return updated;
	}
}
