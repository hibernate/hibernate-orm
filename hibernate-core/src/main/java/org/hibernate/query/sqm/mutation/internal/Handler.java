/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandler;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Simply as a matter of code structuring it is often worthwhile to  put all of the execution code into a separate
 * handler (executor) class.  This contract helps unify those helpers.
 *
 * Hiding this "behind the strategy" also allows mixing approaches based on the nature of specific
 * queries
 *
 * @author Steve Ebersole
 * @deprecated Moved to {@link MultiTableHandler}
 */
@Deprecated(forRemoval = true, since = "7.1")
public interface Handler extends MultiTableHandler {
	/**
	 * Execute the multi-table update or delete indicated by the SQM AST
	 * passed in when this Handler was created.
	 *
	 * @param executionContext Contextual information needed for execution
	 *
	 * @return The "number of rows affected" count
	 */
	default int execute(DomainQueryExecutionContext executionContext) {
		return execute( createJdbcParameterBindings( executionContext ), executionContext );
	}

	@Override
	default boolean dependsOnParameterBindings() {
		return true;
	}

	@Override
	default boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		return false;
	}
}
