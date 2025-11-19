/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of session auto-flush events.
 *
 * @author Steve Ebersole
 */
public interface AutoFlushEventListener {

	/** Handle the given auto-flush event.
	 *
	 * @param event The auto-flush event to be handled.
	 */
	void onAutoFlush(AutoFlushEvent event) throws HibernateException;
}
