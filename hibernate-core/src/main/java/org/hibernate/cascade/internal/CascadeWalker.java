/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import org.hibernate.HibernateException;
import org.hibernate.cascade.spi.CascadePropertySelection;

/// Metadata-driven walker for one entity-rooted cascade traversal.
///
/// The walker receives invocation-local state created by the cascade
/// coordinator and does not retain the context after traversal.
///
/// @author Steve Ebersole
final class CascadeWalker {
	private static final CascadeWalker INSTANCE = new CascadeWalker();

	private CascadeWalker() {
	}

	static CascadeWalker instance() {
		return INSTANCE;
	}

	<T> void traverse(CascadeTraversalContext<T> context) throws HibernateException {
		traverse( context, null );
	}

	<T> void traverse(
			CascadeTraversalContext<T> context,
			CascadePropertySelection propertySelection) throws HibernateException {
		try {
			Cascade.traverseMetadata( context, propertySelection );
		}
		catch (RuntimeException | Error failure) {
			context.traceFailure( failure );
			throw failure;
		}
	}
}
