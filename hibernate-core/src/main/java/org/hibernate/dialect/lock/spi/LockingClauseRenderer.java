/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.SPI;

/// Renders a complete pessimistic locking clause from a focused request.
///
/// The returned fragment includes any leading whitespace needed to separate it
/// from the preceding SQL. An empty fragment indicates that no locking clause
/// should be rendered.
///
/// @see LockingSupport#getLockingClauseRenderer()
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface LockingClauseRenderer {
	/// A renderer for profiles without statement-level locking clauses.
	LockingClauseRenderer NO_OP = request -> "";

	/// Render the locking clause for the given request.
	String render(LockingClauseRequest request);
}
