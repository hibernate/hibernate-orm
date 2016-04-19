/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import javax.transaction.SystemException;

import org.hibernate.TransactionException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;

/**
 * @author Steve Ebersole
 */
public class ExceptionMapperStandardImpl implements ExceptionMapper {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( ExceptionMapperStandardImpl.class );

	public static final ExceptionMapper INSTANCE = new ExceptionMapperStandardImpl();

	@Override
	public RuntimeException mapStatusCheckFailure(
			String message,
			SystemException systemException,
			SessionImplementor sessionImplementor) {
		return new TransactionException(
				"could not determine transaction status in beforeCompletion()",
				systemException
		);
	}

	@Override
	public RuntimeException mapManagedFlushFailure(
			String message,
			RuntimeException failure,
			SessionImplementor session) {
		log.unableToPerformManagedFlush( failure.getMessage() );
		return failure;
	}
}
