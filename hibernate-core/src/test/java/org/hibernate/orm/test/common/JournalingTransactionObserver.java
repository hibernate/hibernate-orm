/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.common;

import org.hibernate.resource.transaction.spi.TransactionObserver;

/**
* @author Steve Ebersole
*/
public class JournalingTransactionObserver implements TransactionObserver {
	private int begins = 0;
	private int beforeCompletions = 0;
	private int afterCompletions = 0;

	@Override
	public void afterBegin() {
		begins++;
	}

	@Override
	public void beforeCompletion() {
		beforeCompletions++;
	}

	@Override
	public void afterCompletion(boolean successful, boolean delayed) {
		afterCompletions++;
	}

	public int getBegins() {
		return begins;
	}

	public int getBeforeCompletions() {
		return beforeCompletions;
	}

	public int getAfterCompletions() {
		return afterCompletions;
	}
}
