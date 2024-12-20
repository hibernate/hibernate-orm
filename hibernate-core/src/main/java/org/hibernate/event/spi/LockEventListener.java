/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of lock events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface LockEventListener {

	/** Handle the given lock event.
	 *
	 * @param event The lock event to be handled.
	 */
	void onLock(LockEvent event) throws HibernateException;
}
