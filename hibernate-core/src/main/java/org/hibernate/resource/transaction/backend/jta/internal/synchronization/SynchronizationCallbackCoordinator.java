/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import jakarta.transaction.Synchronization;

/**
 * Manages funneling JTA Synchronization callbacks back into the Hibernate transaction engine.
 *
 * @author Steve Ebersole
 */
public interface SynchronizationCallbackCoordinator extends Synchronization {
	/**
	 * Called by the TransactionCoordinator when it registers the Synchronization with the JTA system
	 */
	void synchronizationRegistered();

	/**
	 * Called by the TransactionCoordinator to allow the SynchronizationCallbackCoordinator to process any
	 * after-completion handling that it may have delayed due to thread affinity
	 */
	void processAnyDelayedAfterCompletion();
}
