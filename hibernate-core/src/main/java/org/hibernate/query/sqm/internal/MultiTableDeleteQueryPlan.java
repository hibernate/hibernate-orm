/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;

/**
 * @author Steve Ebersole
 */
public class MultiTableDeleteQueryPlan implements NonSelectQueryPlan {
	private final SqmDeleteStatement sqmDelete;
	private final DomainParameterXref domainParameterXref;
	private final SqmMultiTableMutationStrategy deleteStrategy;

	public MultiTableDeleteQueryPlan(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref,
			SqmMultiTableMutationStrategy deleteStrategy) {
		this.sqmDelete = sqmDelete;
		this.domainParameterXref = domainParameterXref;
		this.deleteStrategy = deleteStrategy;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmDelete );
		return deleteStrategy.executeDelete( sqmDelete, domainParameterXref, executionContext );
	}
}
