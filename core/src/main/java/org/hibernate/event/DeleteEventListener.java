//$Id: DeleteEventListener.java 9944 2006-05-24 21:14:56Z steve.ebersole@jboss.com $
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;
import java.util.Set;

/**
 * Defines the contract for handling of deletion events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface DeleteEventListener extends Serializable {

    /** Handle the given delete event.
     *
     * @param event The delete event to be handled.
     * @throws HibernateException
     */
	public void onDelete(DeleteEvent event) throws HibernateException;

	public void onDelete(DeleteEvent event, Set transientEntities) throws HibernateException;
}
