/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.internal;

import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.event.spi.CallbackRegistry;

/**
 * Historically, the listeners for the post-commit events simply reused the
 * contracts from the non-post-commit variants.  That has changed as part of
 * the work for HHH-1582.  The purpose of this specialized EventListenerGroup
 * implementation is to recognize cases where post-commit event listeners
 * are still using the legacy contracts and to issue "deprecation warnings".
 *
 * @author Steve Ebersole
 */
class PostCommitEventListenerGroupImpl<T> extends EventListenerGroupImpl<T> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( PostCommitEventListenerGroupImpl.class );

	private final Class extendedListenerContract;

	public PostCommitEventListenerGroupImpl(
			EventType<T> eventType,
			CallbackRegistry callbackRegistry,
			boolean isJpaBootstrap) {
		super( eventType, callbackRegistry, isJpaBootstrap );

		if ( eventType == EventType.POST_COMMIT_DELETE ) {
			this.extendedListenerContract = PostCommitDeleteEventListener.class;
		}
		else if ( eventType == EventType.POST_COMMIT_INSERT ) {
			this.extendedListenerContract = PostCommitInsertEventListener.class;
		}
		else if ( eventType == EventType.POST_COMMIT_UPDATE ) {
			this.extendedListenerContract = PostCommitUpdateEventListener.class;
		}
		else {
			throw new IllegalStateException( "Unexpected usage of PostCommitEventListenerGroupImpl" );
		}
	}

	@Override
	public void appendListener(T listener) {
		checkAgainstExtendedContract( listener );
		super.appendListener( listener );
	}

	private void checkAgainstExtendedContract(T listener) {
		if ( !extendedListenerContract.isInstance( listener ) ) {
			log.warnf(
					"Encountered event listener [%s] for post-commit event [%s] "
							+ "which did not implement the corresponding extended "
							+ "listener contract [%s]",
					listener.getClass().getName(),
					getEventType().eventName(),
					extendedListenerContract.getName()
			);
		}
	}

	@Override
	public void prependListener(T listener) {
		checkAgainstExtendedContract( listener );
		super.prependListener( listener );
	}
}
