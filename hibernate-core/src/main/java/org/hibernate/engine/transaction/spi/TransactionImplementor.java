/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.spi;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * @author Steve Ebersole
 */
public interface TransactionImplementor extends Transaction {
	/**
	 * Indicate whether a resource transaction is in progress.
	 *
	 * @param isMarkedRollbackConsideredActive whether to consider {@link TransactionStatus#MARKED_ROLLBACK} as active.
	 *
	 * @return boolean indicating whether transaction is in progress
	 * @throws HibernateException if an unexpected error condition is encountered.
	 */
	boolean isActive(boolean isMarkedRollbackConsideredActive);
}
