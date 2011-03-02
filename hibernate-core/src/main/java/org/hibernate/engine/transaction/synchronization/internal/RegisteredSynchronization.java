/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.synchronization.internal;

import org.hibernate.engine.transaction.synchronization.spi.SynchronizationCallbackCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Synchronization;

/**
 * The JTA {@link javax.transaction.Synchronization} Hibernate registers when needed for JTA callbacks
 *
 * @author Steve Ebersole
 */
public class RegisteredSynchronization implements Synchronization {
	private static final Logger log = LoggerFactory.getLogger( RegisteredSynchronization.class );

	private final SynchronizationCallbackCoordinator synchronizationCallbackCoordinator;

	public RegisteredSynchronization(SynchronizationCallbackCoordinator synchronizationCallbackCoordinator) {
		this.synchronizationCallbackCoordinator = synchronizationCallbackCoordinator;
	}

	/**
	 * {@inheritDoc}
	 */
	public void beforeCompletion() {
		log.trace( "JTA sync : beforeCompletion()" );
		synchronizationCallbackCoordinator.beforeCompletion();
	}

	/**
	 * {@inheritDoc}
	 */
	public void afterCompletion(int status) {
		log.trace( "JTA sync : afterCompletion({})", status );
		synchronizationCallbackCoordinator.afterCompletion( status );
	}
}
