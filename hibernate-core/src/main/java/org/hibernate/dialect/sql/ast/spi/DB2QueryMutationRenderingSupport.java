/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

/// DB2-family query-mutation rendering policy.
///
/// A non-native update normally uses merge emulation, but tuple assignment is
/// selected when returning columns require a change-table wrapper because DB2
/// does not permit merge in that wrapper.
///
/// @since 8.0
/// @author Steve Ebersole
public enum DB2QueryMutationRenderingSupport implements QueryMutationRenderingSupport {
	INSTANCE;

	@Override
	public UpdateRenderingPlan determineUpdatePlan(UpdateRenderingRequest request) {
		Objects.requireNonNull( request, "request" );
		if ( !request.hasNonTrivialFromClause()
				|| request.mutationSyntaxSupport().supports(
						MutationKind.UPDATE,
						MutationSyntaxCapability.FROM_CLAUSE
				) ) {
			return new UpdateRenderingPlan.Direct();
		}
		return request.hasReturningColumns()
				? new UpdateRenderingPlan.TupleSet()
				: new UpdateRenderingPlan.Merge();
	}

	@Override
	public DeleteRenderingPlan determineDeletePlan(DeleteRenderingRequest request) {
		return StandardQueryMutationRenderingSupport.STANDARD.determineDeletePlan( request );
	}
}
