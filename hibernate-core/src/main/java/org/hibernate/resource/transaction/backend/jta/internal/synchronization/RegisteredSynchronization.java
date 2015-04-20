/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
