/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Defines the default flush event listeners used by hibernate for
 * flushing session state in response to generated auto-flush events.
 *
 * @author Steve Ebersole
 */
public class DefaultAutoFlushEventListener extends AbstractFlushingEventListener implements AutoFlushEventListener {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, DefaultAutoFlushEventListener.class.getName() );

	/**
	 * Handle the given auto-flush event.
	 * 
	 * @param event
	 *            The auto-flush event to be handled.
	 * @throws HibernateException
	 */
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		final EventSource source = event.getSession();
		try {
			source.getEventListenerManager().partialFlushStart();

			if ( flushMightBeNeeded(source) ) {
				// Need to get the number of collection removals before flushing to executions
				// (because flushing to executions can add collection removal actions to the action queue).
				final int oldSize = source.getActionQueue().numberOfCollectionRemovals();
				flushEverythingToExecutions(event);
				if ( flushIsReallyNeeded(event, source) ) {
					LOG.trace( "Need to execute flush" );

					// note: performExecutions() clears all collectionXxxxtion
					// collections (the collection actions) in the session
					performExecutions(source);
					postFlush(source);

					postPostFlush( source );

					if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
						source.getFactory().getStatisticsImplementor().flush();
					}
				}
				else {
					LOG.trace( "Don't need to execute flush" );
					source.getActionQueue().clearFromFlushNeededCheck( oldSize );
				}

				event.setFlushRequired( flushIsReallyNeeded( event, source ) );
			}
		}
		finally {
			source.getEventListenerManager().partialFlushEnd(
					event.getNumberOfEntitiesProcessed(),
					event.getNumberOfEntitiesProcessed()
			);
		}
	}

	private boolean flushIsReallyNeeded(AutoFlushEvent event, final EventSource source) {
		return source.getActionQueue()
				.areTablesToBeUpdated( event.getQuerySpaces() ) ||
						source.getFlushMode()==FlushMode.ALWAYS;
	}

	private boolean flushMightBeNeeded(final EventSource source) {
		return !source.getFlushMode().lessThan(FlushMode.AUTO) &&
				source.getDontFlushFromFind() == 0 &&
				( source.getPersistenceContext().getNumberOfManagedEntities() > 0 ||
						source.getPersistenceContext().getCollectionEntries().size() > 0 );
	}
}
