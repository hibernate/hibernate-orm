/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.common;

import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.spi.TransactionObserver;

/**
* @author Steve Ebersole
*/
public class JournalingTransactionObserver implements TransactionObserver {
	private int begins = 0;
	private int beforeCompletions = 0;
	private int afterCompletions = 0;

	public void afterBegin() {
		begins++;
	}

	public void beforeCompletion() {
		beforeCompletions++;
	}

	public void afterCompletion(boolean successful) {
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
