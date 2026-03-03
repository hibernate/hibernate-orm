/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperation;

/// Allows supplying a custom [JdbcValueBindings] into the executor.
/// This is useful for deferred handling of insert-generated identifiers (IDENTITY, e.g.).
///
/// @author Steve Ebersole
@FunctionalInterface
public interface JdbcValueBindingsFactory {
	/// Create the [JdbcValueBindings] to use for processing the given `group`.
	JdbcValueBindings create(
			MutationOperation operation,
			JdbcValueDescriptorAccess jdbcValueDescriptorAccess,
			SharedSessionContractImplementor session);
}
