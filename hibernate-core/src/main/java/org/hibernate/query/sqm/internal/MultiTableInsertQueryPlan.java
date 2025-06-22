/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;

/**
 * @author Christian Beikov
 */
public class MultiTableInsertQueryPlan implements NonSelectQueryPlan {
	private final SqmInsertStatement<?> sqmInsert;
	private final DomainParameterXref domainParameterXref;
	private final SqmMultiTableInsertStrategy mutationStrategy;

	public MultiTableInsertQueryPlan(
			SqmInsertStatement<?> sqmInsert,
			DomainParameterXref domainParameterXref,
			SqmMultiTableInsertStrategy mutationStrategy) {
		this.sqmInsert = sqmInsert;
		this.domainParameterXref = domainParameterXref;
		this.mutationStrategy = mutationStrategy;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmInsert );
		return mutationStrategy.executeInsert( sqmInsert, domainParameterXref, executionContext );
	}
}
