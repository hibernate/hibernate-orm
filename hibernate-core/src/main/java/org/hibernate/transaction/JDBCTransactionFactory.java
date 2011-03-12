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
package org.hibernate.transaction;

import java.util.Properties;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.jdbc.JDBCContext;

/**
 * Factory for {@link JDBCTransaction} instances.
 *
 * @author Anton van Straaten
 */
public final class JDBCTransactionFactory implements TransactionFactory {

	/**
	 * {@inheritDoc}
	 */
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_TRANSACTION;
	}

	/**
	 * {@inheritDoc}
	 */
	public Transaction createTransaction(JDBCContext jdbcContext, Context transactionContext)
	throws HibernateException {
		return new JDBCTransaction( jdbcContext, transactionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	public void configure(Properties props) throws HibernateException {}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTransactionManagerRequired() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean areCallbacksLocalToHibernateTransactions() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTransactionInProgress(
			JDBCContext jdbcContext,
	        Context transactionContext,
	        Transaction transaction) {
//		try {
//			// for JDBC-based transactions, we only want to return true
//			// here if we (this transaction) are managing the transaction
//			return transaction != null &&
//			       transaction.isActive() &&
//			       !jdbcContext.getConnectionManager().isAutoCommit();
//		}
//		catch ( SQLException e ) {
//			// assume we are in auto-commit!
//			return false;
//		}
		return transaction != null && transaction.isActive();
	}

}
