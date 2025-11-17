/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
