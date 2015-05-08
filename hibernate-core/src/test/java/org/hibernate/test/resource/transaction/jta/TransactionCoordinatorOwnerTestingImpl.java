/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.resource.transaction.jta;

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
