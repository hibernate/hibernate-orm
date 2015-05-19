/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.common;

import org.hibernate.engine.transaction.spi.TransactionObserver;

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
