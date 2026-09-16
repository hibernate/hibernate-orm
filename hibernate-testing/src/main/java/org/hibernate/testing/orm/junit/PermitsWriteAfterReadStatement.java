/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.testing.orm.ConcurrencyCheckResult;
import org.hibernate.testing.orm.TransactionConcurrencyChecks;

/// Requires that a completed ordinary read permits a competing write while its transaction remains open.
/// This checks exclusion on the same existing row only. A match does not promise
/// successful execution or commit: serialization conflicts, other locks, and
/// database errors may still fail either transaction. Dirty-read protection is
/// a separate prerequisite.
///
/// @see TransactionConcurrencyChecks#permitsWriteAfterReadStatement(TransactionConcurrency)
/// @since 8.0
/// @author Steve Ebersole
public final class PermitsWriteAfterReadStatement implements TransactionConcurrencyFeatureCheck {
	@Override
	public ConcurrencyCheckResult evaluate(TransactionConcurrency concurrency) {
		return TransactionConcurrencyChecks.permitsWriteAfterReadStatement( concurrency );
	}
}
