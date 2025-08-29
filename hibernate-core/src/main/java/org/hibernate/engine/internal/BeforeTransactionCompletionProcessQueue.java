/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;

/**
 * Encapsulates behavior needed for before transaction processing
 */
public class BeforeTransactionCompletionProcessQueue
		extends AbstractTransactionCompletionProcessQueue<BeforeCompletionCallback> {

	public BeforeTransactionCompletionProcessQueue(SharedSessionContractImplementor session) {
		super( session );
	}

	public void beforeTransactionCompletion() {
		BeforeCompletionCallback process;
		while ( (process = processes.poll()) != null ) {
			try {
				process.doBeforeTransactionCompletion( session );
			}
			catch (HibernateException he) {
				throw he;
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to perform beforeTransactionCompletion callback: " + e.getMessage(), e );
			}
		}
	}
}
