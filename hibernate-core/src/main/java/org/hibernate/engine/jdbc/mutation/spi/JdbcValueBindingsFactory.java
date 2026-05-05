/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperation;

/// Allows supplying a custom [JdbcValueBindingsImplementor] into the executor.
///
/// @author Steve Ebersole
@FunctionalInterface
public interface JdbcValueBindingsFactory {
	/// Create the [JdbcValueBindingsImplementor] to use for processing the given operation.
	JdbcValueBindingsImplementor create(
			MutationOperation operation,
			JdbcValueDescriptorAccess jdbcValueDescriptorAccess,
			SharedSessionContractImplementor session);
}
