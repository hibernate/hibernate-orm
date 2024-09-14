/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
