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
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Defines the default flush event listeners used by hibernate for
 * flushing session state in response to generated auto-flush events.
 *
 * @author Steve Ebersole
 */
public class DefaultAutoFlushEventListener extends AbstractFlushingEventListener implements AutoFlushEventListener {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, DefaultAutoFlushEventListener.class.getName() );

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

			if ( flushMightBeNeeded( source ) ) {
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
					LOG.trace( "Need to execute flush" );
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
					LOG.trace( "No need to execute flush" );
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

	@Override
	public void onAutoPreFlush(EventSource source) throws HibernateException {
		final var eventListenerManager = source.getEventListenerManager();
		eventListenerManager.prePartialFlushStart();
		final var eventMonitor = source.getEventMonitor();
		final var diagnosticEvent = eventMonitor.beginPrePartialFlush();
		try {
			if ( flushMightBeNeeded( source ) ) {
				preFlush( source, source.getPersistenceContextInternal() );
			}
		}
		finally {
			eventMonitor.completePrePartialFlush( diagnosticEvent, source );
			eventListenerManager.prePartialFlushEnd();
		}
	}

	private boolean flushIsReallyNeeded(AutoFlushEvent event, final EventSource source) {
		return source.getHibernateFlushMode() == FlushMode.ALWAYS
			|| source.getActionQueue().areTablesToBeUpdated( event.getQuerySpaces() );
	}

	private boolean flushMightBeNeeded(final EventSource source) {
		final var persistenceContext = source.getPersistenceContextInternal();
		return !source.getHibernateFlushMode().lessThan( FlushMode.AUTO )
			&& ( persistenceContext.getNumberOfManagedEntities() > 0
				|| persistenceContext.getCollectionEntriesSize() > 0 );
	}
}
