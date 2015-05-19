/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.internal;

import java.util.LinkedHashSet;
import javax.transaction.Synchronization;

import org.hibernate.engine.transaction.spi.SynchronizationRegistry;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Manages a registry of {@link Synchronization Synchronizations}.
 *
 * @author Steve Ebersole
 */
public class SynchronizationRegistryImpl implements SynchronizationRegistry {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, SynchronizationRegistryImpl.class.getName() );

	private LinkedHashSet<Synchronization> synchronizations;

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		if ( synchronization == null ) {
			throw new NullSynchronizationException();
		}

		if ( synchronizations == null ) {
			synchronizations = new LinkedHashSet<Synchronization>();
		}

		boolean added = synchronizations.add( synchronization );
		if ( !added ) {
			LOG.synchronizationAlreadyRegistered( synchronization );
		}
	}

	@Override
	public void notifySynchronizationsBeforeTransactionCompletion() {
		if ( synchronizations != null ) {
			for ( Synchronization synchronization : synchronizations ) {
				try {
					synchronization.beforeCompletion();
				}
				catch ( Throwable t ) {
					LOG.synchronizationFailed( synchronization, t );
				}
			}
		}
	}

	@Override
	public void notifySynchronizationsAfterTransactionCompletion(int status) {
		if ( synchronizations != null ) {
			for ( Synchronization synchronization : synchronizations ) {
				try {
					synchronization.afterCompletion( status );
				}
				catch ( Throwable t ) {
					LOG.synchronizationFailed( synchronization, t );
				}
			}
		}
	}

	/**
	 * Package-protected access to clear registered synchronizations.
	 */
	void clearSynchronizations() {
		if ( synchronizations != null ) {
			synchronizations.clear();
			synchronizations = null;
		}
	}
}
