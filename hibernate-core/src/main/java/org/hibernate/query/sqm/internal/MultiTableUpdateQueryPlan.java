/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @author Steve Ebersole
 */
public class MultiTableUpdateQueryPlan extends AbstractMultiTableMutationQueryPlan<SqmUpdateStatement<?>, SqmMultiTableMutationStrategy> {

	public MultiTableUpdateQueryPlan(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			SqmMultiTableMutationStrategy mutationStrategy) {
		super( sqmUpdate, domainParameterXref, mutationStrategy );
	}

	@Override
	protected MultiTableHandlerBuildResult buildHandler(
			SqmUpdateStatement<?> statement,
			DomainParameterXref domainParameterXref,
			SqmMultiTableMutationStrategy strategy,
			DomainQueryExecutionContext context) {
		return strategy.buildHandler( statement, domainParameterXref, context );
	}
}
