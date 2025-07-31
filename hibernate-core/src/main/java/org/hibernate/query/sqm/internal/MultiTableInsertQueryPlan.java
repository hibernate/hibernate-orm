/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;

/**
 * @author Christian Beikov
 */
public class MultiTableInsertQueryPlan extends AbstractMultiTableMutationQueryPlan<SqmInsertStatement<?>, SqmMultiTableInsertStrategy> {

	public MultiTableInsertQueryPlan(
			SqmInsertStatement<?> sqmInsert,
			DomainParameterXref domainParameterXref,
			SqmMultiTableInsertStrategy mutationStrategy) {
		super( sqmInsert, domainParameterXref, mutationStrategy );
	}

	@Override
	protected MultiTableHandlerBuildResult buildHandler(
			SqmInsertStatement<?> statement,
			DomainParameterXref domainParameterXref,
			SqmMultiTableInsertStrategy strategy,
			DomainQueryExecutionContext context) {
		return strategy.buildHandler( statement, domainParameterXref, context );
	}
}
