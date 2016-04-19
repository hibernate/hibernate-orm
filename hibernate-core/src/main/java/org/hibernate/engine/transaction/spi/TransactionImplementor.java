/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.spi;

import org.hibernate.Transaction;

/**
 * @author Steve Ebersole
 */
public interface TransactionImplementor extends Transaction {
	/**
	 * Invalidates a transaction handle.   This might happen, e.g., when:<ul>
	 *     <li>The transaction is committed</li>
	 *     <li>The transaction is rolled-back</li>
	 *     <li>The session that owns the transaction is closed</li>
	 * </ul>
	 */
	void invalidate();
}
