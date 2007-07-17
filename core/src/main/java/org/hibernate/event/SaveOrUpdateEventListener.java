//$Id: SaveOrUpdateEventListener.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of update events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface SaveOrUpdateEventListener extends Serializable {

    /** 
     * Handle the given update event.
     *
     * @param event The update event to be handled.
     * @throws HibernateException
     */
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException;

}
