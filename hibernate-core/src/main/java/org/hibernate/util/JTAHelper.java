/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.util;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.TransactionException;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * @author Gavin King
 */
public final class JTAHelper {

	private JTAHelper() {}

	public static boolean isRollback(int status) {
		return status==Status.STATUS_MARKED_ROLLBACK ||
		       status==Status.STATUS_ROLLING_BACK ||
		       status==Status.STATUS_ROLLEDBACK;
	}

	public static boolean isInProgress(int status) {
		return status==Status.STATUS_ACTIVE ||
		       status==Status.STATUS_MARKED_ROLLBACK;
	}

	/**
	 * Return true if a JTA transaction is in progress
	 * and false in *every* other cases (including in a JDBC transaction).
	 */
	public static boolean isTransactionInProgress(SessionFactoryImplementor factory) {
		TransactionManager tm = factory.getTransactionManager();
		try {
			return tm != null && isTransactionInProgress( tm.getTransaction() );
		}
		catch (SystemException se) {
			throw new TransactionException( "could not obtain JTA Transaction", se );
		}
	}

	public static boolean isTransactionInProgress(javax.transaction.Transaction tx) throws SystemException {
		return tx != null && JTAHelper.isInProgress( tx.getStatus() );
	}

	public static boolean isMarkedForRollback(int status) {
		return status == Status.STATUS_MARKED_ROLLBACK;
	}

	public static boolean isMarkedForRollback(javax.transaction.Transaction tx) throws SystemException {
		return isMarkedForRollback( tx.getStatus() );
	}
}
