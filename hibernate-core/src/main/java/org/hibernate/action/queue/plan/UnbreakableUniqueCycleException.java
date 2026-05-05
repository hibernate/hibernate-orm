/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.HibernateException;

/// Indicates that a unique-key UPDATE cycle cannot be made schedulable.
/// A unique cycle can be broken only when the graph contains an explicit
/// null-patchable unique edge. Required unique edges cannot be ignored safely,
/// because doing so leaves execution order arbitrary and usually fails later as
/// a database constraint violation.
///
/// @author Steve Ebersole
public class UnbreakableUniqueCycleException extends HibernateException {
	public UnbreakableUniqueCycleException(String message) {
		super( message );
	}
}
