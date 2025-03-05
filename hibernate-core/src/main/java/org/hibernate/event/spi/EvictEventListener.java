/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of evict events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface EvictEventListener {

	/**
	 * Handle the given evict event.
	 *
	 * @param event The evict event to be handled.
	 */
	void onEvict(EvictEvent event) throws HibernateException;
}
