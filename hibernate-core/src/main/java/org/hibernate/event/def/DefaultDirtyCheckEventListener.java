/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.event.DirtyCheckEvent;
import org.hibernate.event.DirtyCheckEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the default dirty-check event listener used by hibernate for
 * checking the session for dirtiness in response to generated dirty-check
 * events.
 *
 * @author Steve Ebersole
 */
public class DefaultDirtyCheckEventListener extends AbstractFlushingEventListener implements DirtyCheckEventListener {

	private static final Logger log = LoggerFactory.getLogger(DefaultDirtyCheckEventListener.class);

    /** Handle the given dirty-check event.
     *
     * @param event The dirty-check event to be handled.
     * @throws HibernateException
     */
	public void onDirtyCheck(DirtyCheckEvent event) throws HibernateException {

		int oldSize = event.getSession().getActionQueue().numberOfCollectionRemovals();

		try {
			flushEverythingToExecutions(event);
			boolean wasNeeded = event.getSession().getActionQueue().hasAnyQueuedActions();
			log.debug( wasNeeded ? "session dirty" : "session not dirty" );
			event.setDirty( wasNeeded );
		}
		finally {
			event.getSession().getActionQueue().clearFromFlushNeededCheck( oldSize );
		}
		
	}
}
