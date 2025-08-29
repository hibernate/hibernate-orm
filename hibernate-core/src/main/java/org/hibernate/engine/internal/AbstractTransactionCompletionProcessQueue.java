/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.CompletionCallback;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Collection of transaction completion {@linkplain CompletionCallback callbacks}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTransactionCompletionProcessQueue<T extends CompletionCallback> {
	protected SharedSessionContractImplementor session;
	// Concurrency handling required when transaction completion process is dynamically registered
	// inside event listener (HHH-7478).
	protected ConcurrentLinkedQueue<@NonNull T> processes = new ConcurrentLinkedQueue<>();

	protected AbstractTransactionCompletionProcessQueue(SharedSessionContractImplementor session) {
		this.session = session;
	}

	public void register(@Nullable T process) {
		if ( process != null ) {
			processes.add( process );
		}
	}

	public boolean hasActions() {
		return !processes.isEmpty();
	}
}
