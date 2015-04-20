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
package org.hibernate.resource.transaction.internal;

import java.util.LinkedHashSet;
import javax.transaction.Synchronization;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.transaction.LocalSynchronizationException;
import org.hibernate.resource.transaction.NullSynchronizationException;
import org.hibernate.resource.transaction.spi.SynchronizationRegistryImplementor;

/**
 * The standard implementation of the SynchronizationRegistry contract
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
			synchronizations = new LinkedHashSet<Synchronization>();
		}

		final boolean added = synchronizations.add( synchronization );
		if ( !added ) {
			log.synchronizationAlreadyRegistered( synchronization );
		}
	}

	@Override
	public void notifySynchronizationsBeforeTransactionCompletion() {
		log.trace( "SynchronizationRegistryStandardImpl.notifySynchronizationsBeforeTransactionCompletion" );

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
		log.tracef(
				"SynchronizationRegistryStandardImpl.notifySynchronizationsAfterTransactionCompletion(%s)",
				status
		);

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
		log.debug( "Clearing local Synchronizations" );

		if ( synchronizations != null ) {
			synchronizations.clear();
		}
	}
}
