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
	 *
	 * @deprecated (since 5.2) as part of effort to consolidate support for JPA and Hibernate SessionFactory, Session, etc
	 * natively, support for local Transaction delegates to remain "valid" after they are committed or rolled-back (and to a
	 * degree after the owning Session is closed) to more closely comply with the JPA spec natively in terms
	 * of allowing that extended access after Session is closed.  Hibernate impls have all been changed to no-op here.
	 */
	@Deprecated
	default void invalidate() {
		// no-op : see @deprecated note
	}
}
