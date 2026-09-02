/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.SPI;

/// Applies a Dialect's pessimistic locking syntax to already-rendered SQL.
///
/// This strategy is intended for native SQL and other legacy completed-SQL
/// paths. SQL AST translators should apply locking while rendering the
/// semantic tree instead.
///
/// Implementations must not parse SQL to invent structural information omitted
/// from [LockingSqlRewriteRequest#targets]. Return
/// [LockingSqlRewriteResult#unsupported] when the supplied structure is
/// insufficient for a safe rewrite. Rewriters supplied by a Dialect may be
/// reused and therefore must not retain request-specific state.
///
/// @see LockingSupport#getLockingSqlRewriter()
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface LockingSqlRewriter {
	/// Attempt to apply the focused locking request to completed SQL.
	///
	/// @return a non-null result explicitly classifying the outcome
	LockingSqlRewriteResult rewrite(LockingSqlRewriteRequest request);
}
