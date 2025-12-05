/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.internal;

import java.util.LinkedHashSet;
import jakarta.transaction.Synchronization;

import org.hibernate.resource.transaction.LocalSynchronizationException;
import org.hibernate.resource.transaction.NullSynchronizationException;
import org.hibernate.resource.transaction.spi.SynchronizationRegistryImplementor;

import static org.hibernate.resource.transaction.internal.SynchronizationLogging.SYNCHRONIZATION_LOGGER;

/**
 * The standard implementation of the {@link org.hibernate.resource.transaction.spi.SynchronizationRegistry} contract.
 *
 * @author Steve Ebersole
 */
public class SynchronizationRegistryStandardImpl implements SynchronizationRegistryImplementor {

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
			SYNCHRONIZATION_LOGGER.synchronizationAlreadyRegistered( synchronization );
		}
	}

	@Override
	public void notifySynchronizationsBeforeTransactionCompletion() {
		SYNCHRONIZATION_LOGGER.notifyingSynchronizationsBefore();
		if ( synchronizations != null ) {
			for ( var synchronization : synchronizations ) {
				try {
					synchronization.beforeCompletion();
				}
				catch (Throwable t) {
					SYNCHRONIZATION_LOGGER.synchronizationFailed( synchronization, t );
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
		SYNCHRONIZATION_LOGGER.notifyingSynchronizationsAfter( status );
		if ( synchronizations != null ) {
			try {
				for ( var synchronization : synchronizations ) {
					try {
						synchronization.afterCompletion( status );
					}
					catch (Throwable t) {
						SYNCHRONIZATION_LOGGER.synchronizationFailed( synchronization, t );
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
		SYNCHRONIZATION_LOGGER.clearingSynchronizations();
		if ( synchronizations != null ) {
			synchronizations.clear();
		}
	}
}
