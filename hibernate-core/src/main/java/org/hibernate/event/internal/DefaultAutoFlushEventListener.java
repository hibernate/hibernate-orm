/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventSource;

import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;


/**
 * Defines the default flush event listeners used by hibernate for
 * flushing session state in response to generated auto-flush events.
 *
 * @author Steve Ebersole
 */
public class DefaultAutoFlushEventListener extends AbstractFlushingEventListener implements AutoFlushEventListener {

	/**
	 * Handle the given auto-flush event.
	 *
	 * @param event The auto-flush event to be handled.
	 */
	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		final var source = event.getSession();
		final var eventListenerManager = source.getEventListenerManager();
		final var eventMonitor = source.getEventMonitor();
		final var partialFlushEvent = eventMonitor.beginPartialFlushEvent();
		try {
			eventListenerManager.partialFlushStart();
			if ( flushMightBeNeeded( event, source ) ) {
				// Need to get the number of collection removals before flushing to executions
				// (because flushing to executions can add collection removal actions to the action queue).
				final var actionQueue = source.getActionQueue();
				final var session = event.getSession();
				final var persistenceContext = session.getPersistenceContextInternal();
				if ( !event.isSkipPreFlush() ) {
					preFlush( session, persistenceContext );
				}
				final int oldSize = actionQueue.numberOfCollectionRemovals();
				flushEverythingToExecutions( event, persistenceContext, session );
				if ( flushIsReallyNeeded( event, source ) ) {
					EVENT_LISTENER_LOGGER.needToExecuteFlush();
					event.setFlushRequired( true );

					// note: performExecutions() clears all collectionXxxxtion
					// collections (the collection actions) in the session
					final var flushEvent = eventMonitor.beginFlushEvent();
					try {
						performExecutions( source );
						postFlush( source );
						postPostFlush( source );
					}
					finally {
						eventMonitor.completeFlushEvent( flushEvent, event, true );
					}
					final var statistics = source.getFactory().getStatistics();
					if ( statistics.isStatisticsEnabled() ) {
						statistics.flush();
					}
				}
				else {
					EVENT_LISTENER_LOGGER.noNeedToExecuteFlush();
					event.setFlushRequired( false );
					actionQueue.clearFromFlushNeededCheck( oldSize );
				}
			}
		}
		finally {
			eventMonitor.completePartialFlushEvent( partialFlushEvent, event );
			eventListenerManager.partialFlushEnd(
					event.getNumberOfEntitiesProcessed(),
					event.getNumberOfEntitiesProcessed()
			);
		}
	}

	static boolean flushIsReallyNeeded(AutoFlushEvent event, EventSource source) {
		return source.getHibernateFlushMode() == FlushMode.ALWAYS
			|| source.getActionQueue().areTablesToBeUpdated( event.getQuerySpaces() );
	}

	static boolean flushMightBeNeeded(AutoFlushEvent event, EventSource source) {
		return flushMightBeNeededForMode( event, source )
			&& nonEmpty( source );
	}

	private static boolean flushMightBeNeededForMode(AutoFlushEvent event, EventSource source) {
		return switch ( source.getHibernateFlushMode() ) {
			case ALWAYS -> true;
			case AUTO -> {
				final var querySpaces = event.getQuerySpaces();
				yield querySpaces == null || !querySpaces.isEmpty();
			}
			case MANUAL, COMMIT -> false;
		};
	}

	private static boolean nonEmpty(EventSource source) {
		final var persistenceContext = source.getPersistenceContextInternal();
		return persistenceContext.getNumberOfManagedEntities() > 0
			|| persistenceContext.getCollectionEntriesSize() > 0;
	}
}
