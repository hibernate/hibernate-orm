/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

/// Maps a stale-state failure reported while checking a JDBC batch result.
///
/// A mapper is associated with one row as it is added to a [GroupedBatch].
/// If row-count verification for that row throws a [StaleStateException],
/// the batch invokes this mapper before propagating the failure.
///
/// Returning `null` means the stale-state condition was handled and should not
/// be propagated.  Returning an exception means the batch should throw that
/// exception instead of the original stale-state exception.  Implementations may
/// return the supplied exception unchanged when no translation is needed.
///
/// @see GroupedBatch#addToBatch(org.hibernate.engine.jdbc.mutation.JdbcValueBindings, org.hibernate.engine.jdbc.mutation.TableInclusionChecker, StaleStateMapper)
///
/// @author Steve Ebersole
@FunctionalInterface
public interface StaleStateMapper {
	/// Map, replace, or suppress a stale-state exception for the associated batch row.
	///
	/// @param staleStateException the stale-state exception raised by row-count verification
	///
	/// @return the exception to throw, or `null` to suppress the stale-state failure
	@Nullable
	HibernateException map(StaleStateException staleStateException);
}
