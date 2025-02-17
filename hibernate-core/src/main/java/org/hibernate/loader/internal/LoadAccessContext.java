/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

/**
 * Context for loader-access objects.
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
	 * Callback to pulse the transaction coordinator
	 */
	void pulseTransactionCoordinator();
	void delayedAfterCompletion();

	/**
	 * Efficiently fire a {@link LoadEvent} with the given type
	 * and return the resulting entity instance or proxy.
	 *
	 * @since 7.0
	 */
	Object load(
			LoadEventListener.LoadType loadType,
			Object id, String entityName,
			LockOptions lockOptions, Boolean readOnly);
}
