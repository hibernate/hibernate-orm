//$Id: ReplicateEventListener.java 4185 2004-08-08 11:24:56Z oneovthafew $
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for handling of replicate events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface ReplicateEventListener extends Serializable {

    /** Handle the given replicate event.
     *
     * @param event The replicate event to be handled.
     * @throws HibernateException
     */
	public void onReplicate(ReplicateEvent event) throws HibernateException;

}
