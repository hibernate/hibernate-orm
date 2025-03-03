/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.common;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * @author Steve Ebersole
 */
public class SynchronizationCollectorImpl implements Synchronization {
	private int beforeCompletionCount;
	private int successfulCompletionCount;
	private int failedCompletionCount;

	@Override
	public void beforeCompletion() {
		beforeCompletionCount++;
	}

	@Override
	public void afterCompletion(int status) {
		if ( status == Status.STATUS_COMMITTED ) {
			successfulCompletionCount++;
		}
		else {
			failedCompletionCount++;
		}
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
		beforeCompletionCount = 0;
		successfulCompletionCount = 0;
		failedCompletionCount = 0;
	}
}
