/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.Incubating;

/**
 * Collection of {@linkplain BeforeCompletionCallback before} and {@linkplain AfterCompletionCallback after}
 * callbacks related to transaction completion.
 *
 * @author Steve Ebersole
 *
 * @since 7.2
 */
@Incubating
public interface TransactionCompletionCallbacks {
	/**
	 * Commonality for {@linkplain BeforeCompletionCallback before} and
	 * {@linkplain AfterCompletionCallback after} callbacks.
	 */
	interface CompletionCallback {
	}

	interface BeforeCompletionCallback extends CompletionCallback {
		/**
		 * Perform whatever processing is encapsulated here before completion of the transaction.
		 *
		 * @param session The session on which the transaction is preparing to complete.
		 */
		void doBeforeTransactionCompletion(SharedSessionContractImplementor session);

	}

	interface AfterCompletionCallback extends CompletionCallback {
		/**
		 * Perform whatever processing is encapsulated here after completion of the transaction.
		 *
		 * @param success Did the transaction complete successfully?  True means it did.
		 * @param session The session on which the transaction is completing.
		 */
		void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session);
	}

	/**
	 * Register a {@code process} (callback) to be performed at the start of transaction completion.
	 *
	 * @param process The callback.
	 */
	void registerCallback(BeforeCompletionCallback process);

	/**
	 * Register a {@code process} (callback) to be performed at the end of transaction completion.
	 *
	 * @param process The callback.
	 */
	void registerCallback(AfterCompletionCallback process);
}
