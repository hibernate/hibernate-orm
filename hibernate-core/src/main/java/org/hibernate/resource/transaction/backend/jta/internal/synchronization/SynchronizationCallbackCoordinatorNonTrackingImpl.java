/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;

import static org.hibernate.resource.transaction.backend.jta.internal.JtaLogging.JTA_LOGGER;

/**
 * Manages callbacks from the {@link jakarta.transaction.Synchronization} registered by Hibernate.
 *
 * @author Steve Ebersole
 */
public class SynchronizationCallbackCoordinatorNonTrackingImpl implements SynchronizationCallbackCoordinator {

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
		JTA_LOGGER.trace( "Synchronization coordinator: beforeCompletion()" );
		if ( target.isActive() ) {
			target.beforeCompletion();
		}
	}

	@Override
	public void afterCompletion(int status) {
		JTA_LOGGER.tracef( "Synchronization coordinator: afterCompletion(status=%s)", status );
		doAfterCompletion( JtaStatusHelper.isCommitted( status ), false );
	}

	protected void doAfterCompletion(boolean successful, boolean delayed) {
		JTA_LOGGER.tracef( "Synchronization coordinator: doAfterCompletion(successful=%s, delayed=%s)",
				successful, delayed );
		try {
			target.afterCompletion( successful, delayed );
		}
		finally {
			reset();
		}
	}

	@Override
	public void processAnyDelayedAfterCompletion() {
	}
}
