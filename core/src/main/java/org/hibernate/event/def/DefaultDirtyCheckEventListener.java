//$Id: DefaultDirtyCheckEventListener.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.event.DirtyCheckEvent;
import org.hibernate.event.DirtyCheckEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines the default dirty-check event listener used by hibernate for
 * checking the session for dirtiness in response to generated dirty-check
 * events.
 *
 * @author Steve Ebersole
 */
public class DefaultDirtyCheckEventListener extends AbstractFlushingEventListener implements DirtyCheckEventListener {

	private static final Log log = LogFactory.getLog(DefaultDirtyCheckEventListener.class);

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
