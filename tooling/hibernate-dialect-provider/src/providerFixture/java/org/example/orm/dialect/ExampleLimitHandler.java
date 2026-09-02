/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;

/// Provider-defined specialization of a supported completed-SQL pagination
/// family.
///
/// @author Steve Ebersole
public final class ExampleLimitHandler extends LimitOffsetLimitHandler {
	public static final ExampleLimitHandler INSTANCE = new ExampleLimitHandler();

	private ExampleLimitHandler() {
	}

	@Override
	protected String offsetOnlyClause(PaginationRequest request) {
		return " offset " + request.parameterMarker( request.jdbcParameterCount() + 1 );
	}
}
