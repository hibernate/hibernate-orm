/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.event.spi.DirtyCheckEvent;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Defines the default dirty-check event listener used by hibernate for
 * checking the session for dirtiness in response to generated dirty-check
 * events.
 *
 * @author Steve Ebersole
 */
public class DefaultDirtyCheckEventListener extends AbstractFlushingEventListener implements DirtyCheckEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultDirtyCheckEventListener.class );

	/**
	 * Handle the given dirty-check event.
	 * 
	 * @param event The dirty-check event to be handled.
	 * @throws HibernateException
	 */
	public void onDirtyCheck(DirtyCheckEvent event) throws HibernateException {
		final ActionQueue actionQueue = event.getSession().getActionQueue();
		int oldSize = actionQueue.numberOfCollectionRemovals();

		try {
			flushEverythingToExecutions(event);
			boolean wasNeeded = actionQueue.hasAnyQueuedActions();
			if ( wasNeeded ) {
				LOG.debug( "Session dirty" );
			}
			else {
				LOG.debug( "Session not dirty" );
			}
			event.setDirty( wasNeeded );
		}
		finally {
			actionQueue.clearFromFlushNeededCheck( oldSize );
		}
	}
}
