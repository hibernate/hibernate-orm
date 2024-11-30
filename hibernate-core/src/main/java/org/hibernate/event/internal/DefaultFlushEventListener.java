/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.stat.spi.StatisticsImplementor;

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
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		final EventManager eventManager = source.getEventManager();
		if ( persistenceContext.getNumberOfManagedEntities() > 0
				|| persistenceContext.getCollectionEntriesSize() > 0 ) {
			final HibernateMonitoringEvent flushEvent = eventManager.beginFlushEvent();
			try {
				source.getEventListenerManager().flushStart();

				flushEverythingToExecutions( event );
				performExecutions( source );
				postFlush( source );
			}
			finally {
				eventManager.completeFlushEvent( flushEvent, event );
				source.getEventListenerManager().flushEnd(
						event.getNumberOfEntitiesProcessed(),
						event.getNumberOfCollectionsProcessed()
				);
			}

			postPostFlush( source );

			final StatisticsImplementor statistics = source.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.flush();
			}
		}
		else if ( source.getActionQueue().hasAnyQueuedActions() ) {
			// execute any queued unloaded-entity deletions
			performExecutions( source );
		}
	}
}
