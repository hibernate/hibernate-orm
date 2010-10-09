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
import org.hibernate.TransactionException;
import org.hibernate.util.JTAHelper;
import org.hibernate.jdbc.JDBCContext;

import javax.transaction.SystemException;

/**
 * Factory for {@link CMTTransaction} instances.
 *
 * @author Gavin King
 */
public class CMTTransactionFactory implements TransactionFactory {

	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}

	public void configure(Properties props) throws HibernateException {}

	public Transaction createTransaction(JDBCContext jdbcContext, Context transactionContext)
	throws HibernateException {
		return new CMTTransaction(jdbcContext, transactionContext);
	}

	public boolean isTransactionManagerRequired() {
		return true;
	}

	public boolean areCallbacksLocalToHibernateTransactions() {
		return false;
	}

	public boolean isTransactionInProgress(
			JDBCContext jdbcContext,
	        Context transactionContext,
	        Transaction transaction) {
		try {
			return JTAHelper.isTransactionInProgress(
					transactionContext.getFactory().getTransactionManager().getTransaction()
			);
		}
		catch( SystemException se ) {
			throw new TransactionException( "Unable to check transaction status", se );
		}

	}

}
