/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @author Steve Ebersole
 */
public class MultiTableUpdateQueryPlan implements NonSelectQueryPlan {
	private final SqmUpdateStatement sqmUpdate;
	private final DomainParameterXref domainParameterXref;
	private final SqmMultiTableMutationStrategy mutationStrategy;

	public MultiTableUpdateQueryPlan(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			SqmMultiTableMutationStrategy mutationStrategy) {
		this.sqmUpdate = sqmUpdate;
		this.domainParameterXref = domainParameterXref;
		this.mutationStrategy = mutationStrategy;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmUpdate );
		return mutationStrategy.executeUpdate( sqmUpdate, domainParameterXref, executionContext );
	}
}
