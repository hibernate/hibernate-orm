/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.common;

import javax.transaction.Status;
import javax.transaction.Synchronization;

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
