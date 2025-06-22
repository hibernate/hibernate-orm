/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.query.spi.DomainQueryExecutionContext;

/**
 * Simply as a matter of code structuring it is often worthwhile to  put all of the execution code into a separate
 * handler (executor) class.  This contract helps unify those helpers.
 *
 * Hiding this "behind the strategy" also allows mixing approaches based on the nature of specific
 * queries
 *
 * @author Steve Ebersole
 */
public interface Handler {
	/**
	 * Execute the multi-table update or delete indicated by the SQM AST
	 * passed in when this Handler was created.
	 *
	 * @param executionContext Contextual information needed for execution
	 *
	 * @return The "number of rows affected" count
	 */
	int execute(DomainQueryExecutionContext executionContext);
}
