/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PreFlushEvent;
import org.hibernate.event.spi.PreFlushEventListener;

public class DefaultPreFlushEventListener extends AbstractFlushingEventListener implements PreFlushEventListener {

	@Override
	public void onAutoPreFlush(PreFlushEvent event) throws HibernateException {
		final var source = event.getEventSource();
		final var eventListenerManager = source.getEventListenerManager();
		eventListenerManager.prePartialFlushStart();
		final var eventMonitor = source.getEventMonitor();
		final var diagnosticEvent = eventMonitor.beginPrePartialFlush();
		try {
			if ( preFlushMightBeNeeded( source )
					&& event.getParameterBindings().hasAnyTransientEntityBindings( source ) ) {
				preFlush( source, source.getPersistenceContextInternal() );
			}
		}
		finally {
			eventMonitor.completePrePartialFlush( diagnosticEvent, source );
			eventListenerManager.prePartialFlushEnd();
		}
	}


	private static boolean preFlushMightBeNeeded(EventSource source) {
		return flushMightBeNeededForMode( source )
			&& nonEmpty( source );
	}

	private static boolean flushMightBeNeededForMode(EventSource source) {
		return switch ( source.getHibernateFlushMode() ) {
			case ALWAYS, AUTO -> true;
			case MANUAL, COMMIT -> false;
		};
	}

	private static boolean nonEmpty(EventSource source) {
		final var persistenceContext = source.getPersistenceContextInternal();
		return persistenceContext.getNumberOfManagedEntities() > 0
			|| persistenceContext.getCollectionEntriesSize() > 0;
	}
}
