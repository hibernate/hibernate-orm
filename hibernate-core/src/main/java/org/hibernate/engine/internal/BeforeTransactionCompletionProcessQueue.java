package org.hibernate.engine.internal;

import jakarta.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;

/**
 * Encapsulates behavior needed for before transaction processing
 */
class BeforeTransactionCompletionProcessQueue
		extends AbstractTransactionCompletionProcessQueue<BeforeCompletionCallback> {

	BeforeTransactionCompletionProcessQueue(SharedSessionContractImplementor session) {
		super( session );
	}

	void beforeTransactionCompletion() {
		BeforeCompletionCallback process;
		while ( (process = processes.poll()) != null ) {
			try {
				process.doBeforeTransactionCompletion( session );
			}
			catch (PersistenceException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to perform beforeTransactionCompletion callback: " + e.getMessage(), e );
			}
		}
	}
}
