/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

/**
 * Context for loader-access objects.  Generally this is equivalent
 * to the Session
 */
@Incubating
@Internal
public interface LoadAccessContext {
	/**
	 * The session from which the load originates
	 */
	SessionImplementor getSession();

	/**
	 * Callback to check whether the session is "active"
	 */
	void checkOpenOrWaitingForAutoClose();

	/**
	 * Callback to pulse the transaction coo
	 */
	void pulseTransactionCoordinator();
	void delayedAfterCompletion();

	void afterOperation(boolean success);

	void fireLoad(LoadEvent event, LoadEventListener.LoadType load);
}
