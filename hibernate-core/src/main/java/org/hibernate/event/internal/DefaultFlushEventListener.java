/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;

import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;

/**
 * Defines the default flush event listeners used by hibernate for
 * flushing session state in response to generated flush events.
 *
 * @author Steve Ebersole
 */
public class DefaultFlushEventListener extends AbstractFlushingEventListener implements FlushEventListener {

	/** Handle the given flush event.
	 *
	 * @param event The flush event to be handled.
	 */
	public void onFlush(FlushEvent event) throws HibernateException {
		final var source = event.getSession();

		final var eventMonitor = source.getEventMonitor();
		final var flushEvent = eventMonitor.beginFlushEvent();

		final var eventListenerManager = source.getEventListenerManager();
		eventListenerManager.flushStart();

		try {
			final var persistenceContext = source.getPersistenceContextInternal();
			if ( persistenceContext.getNumberOfManagedEntities() > 0
					|| persistenceContext.getCollectionEntriesSize() > 0 ) {
				EVENT_LISTENER_LOGGER.executingFlush();
				flushEverythingToExecutions( event );
				performExecutions( source );
				postFlush( source );
				postPostFlush( source );

				final var statistics = source.getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.flush();
				}
			}
			else if ( source.getActionQueue().hasAnyQueuedActions() ) {
				EVENT_LISTENER_LOGGER.executingFlush();
				// execute any queued unloaded-entity deletions
				performExecutions( source );
			}
		}
		finally {
			eventMonitor.completeFlushEvent( flushEvent, event );
			eventListenerManager.flushEnd(
					event.getNumberOfEntitiesProcessed(),
					event.getNumberOfCollectionsProcessed()
			);
		}
	}
}
