/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.Incubating;

/**
 * @since 7.2
 *
 * @author Gavin King
 *
 */
@Incubating // is this separate interface really needed?
public interface TransactionCompletionCallbacksImplementor extends TransactionCompletionCallbacks {
	/**
	 * Are there any registered before-completion callbacks?
	 */
	boolean hasBeforeCompletionCallbacks();

	/**
	 * Are there any registered after-completion callbacks?
	 */
	boolean hasAfterCompletionCallbacks();

	/**
	 * Execute registered before-completion callbacks, if any.
	 */
	void beforeTransactionCompletion();

	/**
	 * Execute registered after-completion callbacks, if any.
	 */
	void afterTransactionCompletion(boolean success);

	/**
	 * Register a cache space to be invalidated after successful transaction completion.
	 */
	void addSpaceToInvalidate(String space);

	/**
	 * Ensure internal queues are initialized for sharing between sessions that share
	 * the same transaction coordinator. Returns this instance for convenience/fluency.
	 */
	TransactionCompletionCallbacksImplementor forSharing();
}
