/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;

/**
 * @author Steve Ebersole
 */
public class TransactionCompletionCallbacksImpl implements TransactionCompletionCallbacks {
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

	public boolean hasBeforeCompletionCallbacks() {
		return beforeTransactionProcesses != null
			&& beforeTransactionProcesses.hasActions();
	}

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

	public boolean hasAfterCompletionCallbacks() {
		return afterTransactionProcesses != null && afterTransactionProcesses.hasActions();
	}

	public void afterTransactionCompletion(boolean success) {
		if ( afterTransactionProcesses != null && afterTransactionProcesses.hasActions() ) {
			afterTransactionProcesses.afterTransactionCompletion( success );
		}
	}

	public void addSpaceToInvalidate(String space) {
		if ( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		afterTransactionProcesses.addSpaceToInvalidate( space );
}

	public TransactionCompletionCallbacksImpl forSharing() {
		if ( beforeTransactionProcesses == null ) {
			beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
		}
		if ( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		return this;
	}
}
