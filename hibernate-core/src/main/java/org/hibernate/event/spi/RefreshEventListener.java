package org.hibernate.event.spi;


import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of refresh events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface RefreshEventListener {

	/**
	 * Handle the given refresh event.
	 *
	 * @param event The refresh event to be handled.
	 */
	void onRefresh(RefreshEvent event) throws HibernateException;

	void onRefresh(RefreshEvent event, RefreshContext refreshedAlready) throws HibernateException;

}
