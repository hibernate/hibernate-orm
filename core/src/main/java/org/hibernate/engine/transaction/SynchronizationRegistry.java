/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction;

import java.util.LinkedHashSet;
import javax.transaction.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;

/**
 * Manages a registry of {@link Synchronization Synchronizations}.
 *
 * @author Steve Ebersole
 */
public class SynchronizationRegistry {
	private static final Logger log = LoggerFactory.getLogger( SynchronizationRegistry.class );

	private LinkedHashSet<Synchronization> synchronizations;

	/**
	 * Register a user {@link Synchronization} callback for this transaction.
	 *
	 * @param synchronization The synchronization callback to register.
	 * 
	 * @throws HibernateException
	 */
	public void registerSynchronization(Synchronization synchronization) {
		if ( synchronization == null ) {
			throw new NullSynchronizationException();
		}

		if ( synchronizations == null ) {
			synchronizations = new LinkedHashSet<Synchronization>();
		}

		boolean added = synchronizations.add( synchronization );
		if ( !added ) {
			log.info( "Synchronization [{}] was already registered", synchronization );
		}
	}

	/**
	 * Delegate {@link Synchronization#beforeCompletion} calls to {@link #registerSynchronization registered}
	 * {@link Synchronization Synchronizations}
	 */
	public void notifySynchronizationsBeforeTransactionCompletion() {
		if ( synchronizations != null ) {
			for ( Synchronization synchronization : synchronizations ) {
				try {
					synchronization.beforeCompletion();
				}
				catch ( Throwable t ) {
					log.error( "exception calling user Synchronization [{}]", synchronization, t );
				}
			}
		}
	}

	/**
	 * Delegate {@link Synchronization#afterCompletion} calls to {@link #registerSynchronization registered}
	 * {@link Synchronization Synchronizations}
	 *
	 * @param status The transaction status (if known) per {@link javax.transaction.Status}
	 */
	public void notifySynchronizationsAfterTransactionCompletion(int status) {
		if ( synchronizations != null ) {
			for ( Synchronization synchronization : synchronizations ) {
				try {
					synchronization.afterCompletion( status );
				}
				catch ( Throwable t ) {
					log.error( "exception calling user Synchronization [{}]", synchronization, t );
				}
			}
		}
	}
}
