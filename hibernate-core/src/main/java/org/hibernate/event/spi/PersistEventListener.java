/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of create events generated from a session.
 *
 * @author Gavin King
 */
public interface PersistEventListener {

	/**
	 * Handle the given create event.
	 *
	 * @param event The create event to be handled.
	 */
	void onPersist(PersistEvent event) throws HibernateException;

	/**
	 * Handle the given create event.
	 *
	 * @param event The create event to be handled.
	 */
	void onPersist(PersistEvent event, PersistContext createdAlready) throws HibernateException;

}
