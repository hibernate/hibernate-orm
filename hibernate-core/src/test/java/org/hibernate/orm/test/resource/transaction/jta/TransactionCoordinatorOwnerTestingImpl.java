/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jta;

import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class TransactionCoordinatorOwnerTestingImpl implements TransactionCoordinatorOwner {

	private static final Logger log = Logger.getLogger( TransactionCoordinatorOwnerTestingImpl.class );

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
	public void startTransactionBoundary() {
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
