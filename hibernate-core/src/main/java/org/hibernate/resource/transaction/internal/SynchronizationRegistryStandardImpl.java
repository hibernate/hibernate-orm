/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.internal;

import java.util.LinkedHashSet;
import jakarta.transaction.Synchronization;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.transaction.LocalSynchronizationException;
import org.hibernate.resource.transaction.NullSynchronizationException;
import org.hibernate.resource.transaction.spi.SynchronizationRegistryImplementor;

/**
 * The standard implementation of the {@link org.hibernate.resource.transaction.spi.SynchronizationRegistry} contract.
 *
 * @author Steve Ebersole
 */
public class SynchronizationRegistryStandardImpl implements SynchronizationRegistryImplementor {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( SynchronizationRegistryStandardImpl.class );

	private LinkedHashSet<Synchronization> synchronizations;

	/**
	 * Intended for test access
	 *
	 * @return The number of Synchronizations registered
	 */
	public int getNumberOfRegisteredSynchronizations() {
		return synchronizations == null ? 0 : synchronizations.size();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		if ( synchronization == null ) {
			throw new NullSynchronizationException();
		}

		if ( synchronizations == null ) {
			synchronizations = new LinkedHashSet<>();
		}

		final boolean added = synchronizations.add( synchronization );
		if ( !added ) {
			log.synchronizationAlreadyRegistered( synchronization );
		}
	}

	@Override
	public void notifySynchronizationsBeforeTransactionCompletion() {
		log.trace( "Notifying Synchronizations (before completion)" );
		if ( synchronizations != null ) {
			for ( Synchronization synchronization : synchronizations ) {
				try {
					synchronization.beforeCompletion();
				}
				catch (Throwable t) {
					log.synchronizationFailed( synchronization, t );
					throw new LocalSynchronizationException(
							"Exception calling user Synchronization (beforeCompletion): " + synchronization.getClass().getName(),
							t
					);
				}
			}
		}
	}

	@Override
	public void notifySynchronizationsAfterTransactionCompletion(int status) {
		log.tracef( "Notifying Synchronizations (after completion with status %s)", status );
		if ( synchronizations != null ) {
			try {
				for ( Synchronization synchronization : synchronizations ) {
					try {
						synchronization.afterCompletion( status );
					}
					catch (Throwable t) {
						log.synchronizationFailed( synchronization, t );
						throw new LocalSynchronizationException(
								"Exception calling user Synchronization (afterCompletion): " + synchronization.getClass().getName(),
								t
						);
					}
				}
			}
			finally {
				clearSynchronizations();
			}
		}
	}

	@Override
	public void clearSynchronizations() {
		log.trace( "Clearing local Synchronizations" );
		if ( synchronizations != null ) {
			synchronizations.clear();
		}
	}
}
