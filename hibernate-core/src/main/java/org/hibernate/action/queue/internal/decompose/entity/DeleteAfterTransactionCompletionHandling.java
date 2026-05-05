/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;

/// After-transaction completion handling for graph-based entity deletes.
///
/// Replaces legacy action-as-callback.
///
/// @author Steve Ebersole
public class DeleteAfterTransactionCompletionHandling
		implements TransactionCompletionCallbacks.AfterCompletionCallback {
	private final EntityDeleteAction action;
	private final DeleteCacheHandling.CacheLock cacheLock;

	public DeleteAfterTransactionCompletionHandling(
			EntityDeleteAction action,
			DeleteCacheHandling.CacheLock cacheLock) {
		this.action = action;
		this.cacheLock = cacheLock;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
		DeleteCacheHandling.unlockItem( action, cacheLock, session );
		postCommitDelete( success, session );
	}

	public boolean isNeeded(SharedSessionContractImplementor session) {
		return action.getPersister().canWriteToCache() || hasPostCommitEventListeners( session );
	}

	private boolean hasPostCommitEventListeners(SharedSessionContractImplementor session) {
		for ( var listener : session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_POST_COMMIT_DELETE
				.listeners() ) {
			if ( listener.requiresPostCommitHandling( action.getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	private void postCommitDelete(boolean success, SharedSessionContractImplementor session) {
		final var eventListeners = session.getFactory().getEventListenerGroups().eventListenerGroup_POST_COMMIT_DELETE;
		if ( success ) {
			eventListeners.fireLazyEventOnEachListener(
					() -> newPostDeleteEvent( session ),
					PostDeleteEventListener::onPostDelete
			);
		}
		else {
			eventListeners.fireLazyEventOnEachListener(
					() -> newPostDeleteEvent( session ),
					DeleteAfterTransactionCompletionHandling::postCommitDeleteOnUnsuccessful
			);
		}
	}

	private PostDeleteEvent newPostDeleteEvent(SharedSessionContractImplementor session) {
		return new PostDeleteEvent(
				action.getInstance(),
				action.getId(),
				action.getState(),
				action.getPersister(),
				session
		);
	}

	private static void postCommitDeleteOnUnsuccessful(PostDeleteEventListener listener, PostDeleteEvent event) {
		if ( listener instanceof PostCommitDeleteEventListener postCommitDeleteEventListener ) {
			postCommitDeleteEventListener.onPostDeleteCommitFailed( event );
		}
		else {
			listener.onPostDelete( event );
		}
	}
}
