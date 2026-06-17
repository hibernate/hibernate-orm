/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.entity;

import org.hibernate.AssertionFailure;
import org.hibernate.Incubating;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;

/// Post-execution callback that performs only the essential persistence
/// context cleanup after a delete action is processed.
///
/// This is the base handling used when the entity was never actually
/// written to the database — for example, when a `persist()` and
/// `remove()` cancel each other out in the same flush cycle.
///
/// The full [PostDeleteHandling] builds on top of this to add cache eviction,
/// natural ID cleanup, event firing, and statistics.
///
/// @see PostDeleteHandling
/// @see EntityDeleteAction
///
/// @since 8.0
@Incubating
public class CancelledInsertPostDeleteHandling implements PostExecutionCallback {
	protected final EntityDeleteAction action;

	public CancelledInsertPostDeleteHandling(EntityDeleteAction action) {
		this.action = action;
	}

	@Override
	public void handle(SessionImplementor session) {
		final Object instance = action.getInstance();
		if ( instance != null ) {
			basicPersistenceContextCleanup( session.getPersistenceContextInternal(), instance );
		}
	}

	static void basicPersistenceContextCleanup(PersistenceContext persistenceContext, Object instance) {
		final var entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to session" );
		}
		entry.postDelete();
		persistenceContext.removeEntityHolder( entry.getEntityKey() );
	}
}
