/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import static org.hibernate.engine.transaction.internal.jta.JtaStatusHelper.isCommitted;
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
		JTA_LOGGER.synchronizationCoordinatorBeforeCompletion();
		if ( target.isActive() ) {
			target.beforeCompletion();
		}
	}

	@Override
	public void afterCompletion(int status) {
		JTA_LOGGER.synchronizationCoordinatorAfterCompletion( status );
		doAfterCompletion( isCommitted( status ), false );
	}

	protected void doAfterCompletion(boolean successful, boolean delayed) {
		JTA_LOGGER.synchronizationCoordinatorDoAfterCompletion( successful, delayed );
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
