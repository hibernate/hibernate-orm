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

import javax.transaction.SystemException;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
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

	private ExceptionMapper exceptionMapper;

	public SynchronizationCallbackCoordinatorNonTrackingImpl(SynchronizationCallbackTarget target) {
		this.target = target;
		reset();
	}

	public void reset() {
		exceptionMapper = STANDARD_EXCEPTION_MAPPER;
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
		try {
			target.beforeCompletion();
		}
		catch (HibernateException he) {
			if ( he.getCause() instanceof SystemException ) {
				throw exceptionMapper.mapStatusCheckFailure( he.getMessage(), (SystemException) he.getCause() );
			}
			else {
				throw exceptionMapper.mapManagedFlushFailure( "error during managed flush", he );
			}

		}
		catch (RuntimeException re) {
			throw exceptionMapper.mapManagedFlushFailure( "error during managed flush", re );
		}
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

	@Override
	public void setExceptionMapper(ExceptionMapper exceptionMapper) {
		this.exceptionMapper = exceptionMapper;
	}

	private static final ExceptionMapper STANDARD_EXCEPTION_MAPPER = new ExceptionMapper() {
		@Override
		public RuntimeException mapStatusCheckFailure(String message, SystemException systemException) {
			return new TransactionException( "could not determine transaction status in beforeCompletion()",
											 systemException );
		}

		@Override
		public RuntimeException mapManagedFlushFailure(String message, RuntimeException failure) {
			log.unableToPerformManagedFlush( failure.getMessage() );
			return failure;
		}
	};
}
