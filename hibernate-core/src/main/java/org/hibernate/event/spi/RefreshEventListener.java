/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;


import org.hibernate.HibernateException;
import jakarta.annotation.Nonnull;

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
	void onRefresh(@Nonnull RefreshEvent event) throws HibernateException;

	void onRefresh(@Nonnull RefreshEvent event, @Nonnull RefreshContext refreshedAlready) throws HibernateException;

}
