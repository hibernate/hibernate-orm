/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;


import org.hibernate.HibernateException;
import jakarta.annotation.Nonnull;

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
	void onDelete(@Nonnull DeleteEvent event) throws HibernateException;

	void onDelete(@Nonnull DeleteEvent event, @Nonnull DeleteContext transientEntities) throws HibernateException;
}
