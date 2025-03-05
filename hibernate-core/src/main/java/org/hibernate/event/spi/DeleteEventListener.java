/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;


import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of deletion events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface DeleteEventListener {

	/**
	 * Handle the given delete event.
	 *
	 * @param event The delete event to be handled.
	 */
	void onDelete(DeleteEvent event) throws HibernateException;

	void onDelete(DeleteEvent event, DeleteContext transientEntities) throws HibernateException;
}
