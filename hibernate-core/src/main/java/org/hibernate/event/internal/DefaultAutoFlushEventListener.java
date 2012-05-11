/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.internal;

import org.jboss.logging.Logger;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;

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
		if ( flushMightBeNeeded(source) ) {
			// Need to get the number of collection removals before flushing to executions
			// (because flushing to executions can add collection removal actions to the action queue).
			final int oldSize = source.getActionQueue().numberOfCollectionRemovals();
			flushEverythingToExecutions(event);
			if ( flushIsReallyNeeded(event, source) ) {
				LOG.trace( "Need to execute flush" );

				performExecutions(source);
				postFlush(source);
				// note: performExecutions() clears all collectionXxxxtion
				// collections (the collection actions) in the session

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

	private boolean flushIsReallyNeeded(AutoFlushEvent event, final EventSource source) {
		return source.getActionQueue()
				.areTablesToBeUpdated( event.getQuerySpaces() ) ||
						source.getFlushMode()==FlushMode.ALWAYS;
	}

	private boolean flushMightBeNeeded(final EventSource source) {
		return !source.getFlushMode().lessThan(FlushMode.AUTO) &&
				source.getDontFlushFromFind() == 0 &&
				( source.getPersistenceContext().getEntityEntries().size() > 0 ||
						source.getPersistenceContext().getCollectionEntries().size() > 0 );
	}
}
