//$Id: AutoFlushEventListener.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for handling of session auto-flush events.
 *
 * @author Steve Ebersole
 */
public interface AutoFlushEventListener extends Serializable {

    /** Handle the given auto-flush event.
     *
     * @param event The auto-flush event to be handled.
     * @throws HibernateException
     */
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException;
}
