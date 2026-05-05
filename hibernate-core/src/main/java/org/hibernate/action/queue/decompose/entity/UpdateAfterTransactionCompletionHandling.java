/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;

/// After-transaction completion handling for graph-based entity updates.
///
/// @author Steve Ebersole
public class UpdateAfterTransactionCompletionHandling
		implements TransactionCompletionCallbacks.AfterCompletionCallback {
	private final EntityUpdateAction action;
	private final UpdateCacheHandling.CacheUpdate cacheUpdate;

	public UpdateAfterTransactionCompletionHandling(
			EntityUpdateAction action,
			UpdateCacheHandling.CacheUpdate cacheUpdate) {
		this.action = action;
		this.cacheUpdate = cacheUpdate;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
		UpdateCacheHandling.afterTransactionCompletion( success, action, cacheUpdate, session );
		postCommitUpdate( success, session );
	}

	public boolean isNeeded(SharedSessionContractImplementor session) {
		return action.getPersister().canWriteToCache() || hasPostCommitEventListeners( session );
	}

	private boolean hasPostCommitEventListeners(SharedSessionContractImplementor session) {
		for ( var listener : session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_POST_COMMIT_UPDATE
				.listeners() ) {
			if ( listener.requiresPostCommitHandling( action.getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	private void postCommitUpdate(boolean success, SharedSessionContractImplementor session) {
		final var eventListeners = session.getFactory().getEventListenerGroups().eventListenerGroup_POST_COMMIT_UPDATE;
		if ( success ) {
			eventListeners.fireLazyEventOnEachListener(
					() -> newPostUpdateEvent( session ),
					PostUpdateEventListener::onPostUpdate
			);
		}
		else {
			eventListeners.fireLazyEventOnEachListener(
					() -> newPostUpdateEvent( session ),
					UpdateAfterTransactionCompletionHandling::onPostCommitFailure
			);
		}
	}

	private PostUpdateEvent newPostUpdateEvent(SharedSessionContractImplementor session) {
		return new PostUpdateEvent(
				action.getInstance(),
				action.getId(),
				action.getState(),
				action.getPreviousState(),
				action.getDirtyFields(),
				action.getPersister(),
				session
		);
	}

	private static void onPostCommitFailure(PostUpdateEventListener listener, PostUpdateEvent event) {
		if ( listener instanceof PostCommitUpdateEventListener postCommitUpdateEventListener ) {
			postCommitUpdateEventListener.onPostUpdateCommitFailed( event );
		}
		else {
			listener.onPostUpdate( event );
		}
	}
}
