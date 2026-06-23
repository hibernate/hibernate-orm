/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PreFlushEvent;
import org.hibernate.event.spi.PreFlushEventListener;
import jakarta.annotation.Nonnull;

public class DefaultPreFlushEventListener extends AbstractFlushingEventListener implements PreFlushEventListener {

	@Override
	public void onAutoPreFlush(@Nonnull PreFlushEvent event) {
		final var source = event.getEventSource();
		final var eventListenerManager = source.getEventListenerManager();
		eventListenerManager.prePartialFlushStart();
		final var eventMonitor = source.getEventMonitor();
		final var diagnosticEvent = eventMonitor.beginPrePartialFlush();
			try {
				if ( preFlushMightBeNeeded( source )
						&& event.getParameterBindings().hasAnyTransientEntityBindings( source ) ) {
					final var persistenceContext = source.getPersistenceContextInternal();
					preFlush( source, persistenceContext, beginFlushProcessing( source, persistenceContext ) );
				}
			}
			finally {
				clearFlushProcessing( source.getPersistenceContextInternal() );
				eventMonitor.completePrePartialFlush( diagnosticEvent, source );
				eventListenerManager.prePartialFlushEnd();
		}
	}


	private static boolean preFlushMightBeNeeded(@Nonnull EventSource source) {
		return flushMightBeNeededForMode( source )
			&& nonEmpty( source );
	}

	private static boolean flushMightBeNeededForMode(@Nonnull EventSource source) {
		return switch ( source.getHibernateFlushMode() ) {
			case ALWAYS, AUTO -> true;
			case MANUAL, COMMIT -> false;
		};
	}

	private static boolean nonEmpty(@Nonnull EventSource source) {
		final var persistenceContext = source.getPersistenceContextInternal();
		return persistenceContext.getNumberOfManagedEntities() > 0
			|| persistenceContext.getCollectionEntriesSize() > 0;
	}
}
