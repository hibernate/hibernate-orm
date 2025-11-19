/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.PreFlushEventListener;

import static org.hibernate.event.internal.DefaultAutoFlushEventListener.flushMightBeNeeded;

public class DefaultPreFlushEventListener extends AbstractFlushingEventListener implements PreFlushEventListener {
	@Override
	public void onAutoPreFlush(AutoFlushEvent event) throws HibernateException {
		final var source = event.getEventSource();
		final var eventListenerManager = source.getEventListenerManager();
		eventListenerManager.prePartialFlushStart();
		final var eventMonitor = source.getEventMonitor();
		final var diagnosticEvent = eventMonitor.beginPrePartialFlush();
		try {
			if ( flushMightBeNeeded( event, source ) ) {
				preFlush( source, source.getPersistenceContextInternal() );
			}
		}
		finally {
			eventMonitor.completePrePartialFlush( diagnosticEvent, source );
			eventListenerManager.prePartialFlushEnd();
		}
	}
}
