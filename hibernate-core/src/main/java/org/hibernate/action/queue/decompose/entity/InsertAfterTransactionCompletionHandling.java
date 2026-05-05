/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;

/// After-transaction completion handling for graph-based entity inserts.
///
/// @author Steve Ebersole
public class InsertAfterTransactionCompletionHandling
		implements TransactionCompletionCallbacks.AfterCompletionCallback {
	private final AbstractEntityInsertAction action;
	private final InsertCacheHandling.CacheInsert cacheInsert;

	public InsertAfterTransactionCompletionHandling(
			AbstractEntityInsertAction action,
			InsertCacheHandling.CacheInsert cacheInsert) {
		this.action = action;
		this.cacheInsert = cacheInsert;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
		InsertCacheHandling.afterTransactionCompletion( success, action, cacheInsert, session );
		postCommitInsert( success, session );
	}

	public boolean isNeeded(SharedSessionContractImplementor session) {
		return action.getPersister().canWriteToCache() || hasPostCommitEventListeners( session );
	}

	private boolean hasPostCommitEventListeners(SharedSessionContractImplementor session) {
		for ( var listener : session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_POST_COMMIT_INSERT
				.listeners() ) {
			if ( listener.requiresPostCommitHandling( action.getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	private void postCommitInsert(boolean success, SharedSessionContractImplementor session) {
		final var eventListeners = session.getFactory().getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT;
		if ( success ) {
			eventListeners.fireLazyEventOnEachListener(
					() -> newPostInsertEvent( session ),
					PostInsertEventListener::onPostInsert
			);
		}
		else {
			eventListeners.fireLazyEventOnEachListener(
					() -> newPostInsertEvent( session ),
					InsertAfterTransactionCompletionHandling::postCommitOnFailure
			);
		}
	}

	private PostInsertEvent newPostInsertEvent(SharedSessionContractImplementor session) {
		return new PostInsertEvent(
				action.getInstance(),
				cacheInsert.id() == null ? action.getId() : cacheInsert.id(),
				action.getState(),
				action.getPersister(),
				session
		);
	}

	private static void postCommitOnFailure(PostInsertEventListener listener, PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener postCommitInsertEventListener ) {
			postCommitInsertEventListener.onPostInsertCommitFailed( event );
		}
		else {
			listener.onPostInsert( event );
		}
	}
}
