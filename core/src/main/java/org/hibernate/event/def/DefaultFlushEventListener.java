//$Id: DefaultFlushEventListener.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.event.EventSource;
import org.hibernate.event.FlushEvent;
import org.hibernate.event.FlushEventListener;

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
		if ( source.getPersistenceContext().hasNonReadOnlyEntities() ) {
			
			flushEverythingToExecutions(event);
			performExecutions(source);
			postFlush(source);
		
			if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
				source.getFactory().getStatisticsImplementor().flush();
			}
			
		}
	}
}
