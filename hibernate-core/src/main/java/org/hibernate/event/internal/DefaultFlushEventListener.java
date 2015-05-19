/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;

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
	 * @throws HibernateException
	 */
	public void onFlush(FlushEvent event) throws HibernateException {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContext();

		if ( persistenceContext.getNumberOfManagedEntities() > 0 ||
				persistenceContext.getCollectionEntries().size() > 0 ) {

			try {
				source.getEventListenerManager().flushStart();

				flushEverythingToExecutions( event );
				performExecutions( source );
				postFlush( source );
			}
			finally {
				source.getEventListenerManager().flushEnd(
						event.getNumberOfEntitiesProcessed(),
						event.getNumberOfCollectionsProcessed()
				);
			}

			postPostFlush( source );

			if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
				source.getFactory().getStatisticsImplementor().flush();
			}
		}
	}
}
