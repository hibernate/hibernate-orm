/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jdbc;

import org.hibernate.HibernateException;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class TransactionCoordinatorOwnerTestingImpl
		implements TransactionCoordinatorOwner, JdbcResourceTransactionAccess {

	private static final Logger log = Logger.getLogger( TransactionCoordinatorOwnerTestingImpl.class );

	private LogicalConnectionTestingImpl logicalConnection;

	private boolean active;

	private int beginCount;
	private int beforeCompletionCount;
	private int successfulCompletionCount;
	private int failedCompletionCount;

	public TransactionCoordinatorOwnerTestingImpl() {
		this( true );
	}

	public TransactionCoordinatorOwnerTestingImpl(boolean active) {
		this.active = active;
		try {
			this.logicalConnection = new LogicalConnectionTestingImpl();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not create logical connection", e );
		}
	}


	@Override
	public JdbcResourceTransaction getResourceLocalTransaction() {
		return logicalConnection;
	}

	public int getBeforeCompletionCount() {
		return beforeCompletionCount;
	}

	public int getSuccessfulCompletionCount() {
		return successfulCompletionCount;
	}

	public int getFailedCompletionCount() {
		return failedCompletionCount;
	}

	public void reset() {
		beginCount = 0;
		beforeCompletionCount = 0;
		successfulCompletionCount = 0;
		failedCompletionCount = 0;
	}

	public void deactivate() {
		active = false;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void afterTransactionBegin() {
		log.debug( "#afterTransactionBegin called" );
		beginCount++;
	}

	@Override
	public void beforeTransactionCompletion() {
		log.debug( "#beforeTransactionCompletion called" );
		beforeCompletionCount++;
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		log.debug( "#afterTransactionCompletion called" );
		if ( successful ) {
			successfulCompletionCount++;
		}
		else {
			failedCompletionCount++;
		}

	}

	@Override
	public JdbcSessionOwner getJdbcSessionOwner() {
		return null;
	}

	@Override
	public void setTransactionTimeOut(int seconds) {

	}

	@Override
	public void flushBeforeTransactionCompletion() {
	}
}
