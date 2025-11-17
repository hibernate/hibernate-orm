/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;

/**
 * @author Steve Ebersole
 */
public class TransactionCompletionCallbacksImpl implements TransactionCompletionCallbacksImplementor {
	private final SharedSessionContractImplementor session;

	private BeforeTransactionCompletionProcessQueue beforeTransactionProcesses;
	private AfterTransactionCompletionProcessQueue afterTransactionProcesses;

	public TransactionCompletionCallbacksImpl(SharedSessionContractImplementor session) {
		this.session = session;
	}

	@Override
	public void registerCallback(BeforeCompletionCallback process) {
		if ( beforeTransactionProcesses == null ) {
			beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
		}
		beforeTransactionProcesses.register( process );
	}

	@Override
	public boolean hasBeforeCompletionCallbacks() {
		return beforeTransactionProcesses != null
			&& beforeTransactionProcesses.hasActions();
	}

	@Override
	public void beforeTransactionCompletion() {
		if ( beforeTransactionProcesses != null && beforeTransactionProcesses.hasActions() ) {
			beforeTransactionProcesses.beforeTransactionCompletion();
		}
	}

	@Override
	public void registerCallback(AfterCompletionCallback process) {
		if ( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		afterTransactionProcesses.register( process );
	}

	@Override
	public boolean hasAfterCompletionCallbacks() {
		return afterTransactionProcesses != null && afterTransactionProcesses.hasActions();
	}

	@Override
	public void afterTransactionCompletion(boolean success) {
		if ( afterTransactionProcesses != null && afterTransactionProcesses.hasActions() ) {
			afterTransactionProcesses.afterTransactionCompletion( success );
		}
	}

	@Override
	public void addSpaceToInvalidate(String space) {
		if ( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		afterTransactionProcesses.addSpaceToInvalidate( space );
	}

	@Override
	public TransactionCompletionCallbacksImpl forSharing() {
		if ( beforeTransactionProcesses == null ) {
			beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
		}
		if ( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		return this;
	}

	@Override
	public void executePendingBulkOperationCleanUpActions() {
		if ( afterTransactionProcesses != null ) {
			afterTransactionProcesses.executePendingBulkOperationCleanUpActions();
		}
	}
}
