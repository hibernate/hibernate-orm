/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of session flush events.
 *
 * @author Steve Ebersole
 */
public interface FlushEventListener {
	/**
	 * Handle the given flush event.
	 *
	 * @param event The flush event to be handled.
	 */
	void onFlush(FlushEvent event) throws HibernateException;
}
