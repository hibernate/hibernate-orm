/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Manages callbacks from the {@link javax.transaction.Synchronization} registered by Hibernate.
 * 
 * @author Steve Ebersole
 */
public class SynchronizationCallbackCoordinatorNonTrackingImpl implements SynchronizationCallbackCoordinator {
	private static final CoreMessageLogger log = CoreLogging.messageLogger(
			SynchronizationCallbackCoordinatorNonTrackingImpl.class
	);

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
		log.tracef( "Synchronization coordinator: afterCompletion(status=%s)", status );
		doAfterCompletion( JtaStatusHelper.isCommitted( status ), false );
	}

	protected void doAfterCompletion(boolean successful, boolean delayed) {
		log.tracef( "Synchronization coordinator: doAfterCompletion(successful=%s, delayed=%s)", successful, delayed );

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
