/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import javax.transaction.Synchronization;

import org.jboss.logging.Logger;

import static org.hibernate.internal.CoreLogging.logger;

/**
 * The JTA {@link javax.transaction.Synchronization} Hibernate registers when needed for JTA callbacks.
 * <p/>
 * Note that we split the notion of the registered Synchronization and the processing of the Synchronization callbacks
 * mainly to account for "separation of concerns", but also so that the transaction engine does not have to hold
 * reference to the actual Synchronization that gets registered with the JTA system.
 *
 * @author Steve Ebersole
 */
public class RegisteredSynchronization implements Synchronization {
	private static final Logger log = logger( RegisteredSynchronization.class );

	private final SynchronizationCallbackCoordinator synchronizationCallbackCoordinator;

	public RegisteredSynchronization(SynchronizationCallbackCoordinator synchronizationCallbackCoordinator) {
		this.synchronizationCallbackCoordinator = synchronizationCallbackCoordinator;
	}

	@Override
	public void beforeCompletion() {
		log.trace( "Registered JTA Synchronization : beforeCompletion()" );

		synchronizationCallbackCoordinator.beforeCompletion();
	}

	@Override
	public void afterCompletion(int status) {
		log.tracef( "Registered JTA Synchronization : afterCompletion(%s)", status );

		synchronizationCallbackCoordinator.afterCompletion( status );
	}
}
