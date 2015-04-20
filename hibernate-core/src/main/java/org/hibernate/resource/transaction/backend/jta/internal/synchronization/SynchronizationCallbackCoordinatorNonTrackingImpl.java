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

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;

import org.jboss.logging.Logger;

import static org.hibernate.internal.CoreLogging.logger;

/**
 * Manages callbacks from the {@link javax.transaction.Synchronization} registered by Hibernate.
 * 
 * @author Steve Ebersole
 */
public class SynchronizationCallbackCoordinatorNonTrackingImpl implements SynchronizationCallbackCoordinator {
	private static final Logger log = logger( SynchronizationCallbackCoordinatorNonTrackingImpl.class );

	private final SynchronizationCallbackTarget target;

	public SynchronizationCallbackCoordinatorNonTrackingImpl(SynchronizationCallbackTarget target) {
		this.target = target;
		reset();
	}

	public void reset() {
	}

	@Override
	public void synchronizationRegistered() {
		// Nothing to do here
	}

	// sync callbacks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void beforeCompletion() {
		log.trace( "Synchronization coordinator: beforeCompletion()" );

		if ( !target.isActive() ) {
			return;
		}

		target.beforeCompletion();
	}


	@Override
	public void afterCompletion(int status) {
		doAfterCompletion( JtaStatusHelper.isCommitted( status ) );
	}

	protected void doAfterCompletion(boolean successful) {
		try {
			target.afterCompletion( successful );
		}
		finally {
			reset();
		}
	}

	@Override
	public void processAnyDelayedAfterCompletion() {
	}
}
