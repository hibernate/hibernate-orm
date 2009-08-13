/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.ejb.transaction;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.transaction.CMTTransactionFactory;

/**
 * A transaction is in progress if the underlying JTA tx is in progress and if the Tx is marked as
 * MARKED_FOR_JOINED
 *
 * @author Emmanuel Bernard
 */
public class JoinableCMTTransactionFactory extends CMTTransactionFactory {
	public Transaction createTransaction(
			JDBCContext jdbcContext,
			Context transactionContext) throws HibernateException {
		return new JoinableCMTTransaction( jdbcContext, transactionContext );
	}

	@Override
	public boolean isTransactionInProgress(
			JDBCContext jdbcContext,
			Context transactionContext,
			Transaction transaction) {
		if ( transaction == null ) {
			return false; //should not happen though
		}
		JoinableCMTTransaction joinableCMTTransaction = ( (JoinableCMTTransaction) transaction );
		joinableCMTTransaction.tryJoiningTransaction();
		return joinableCMTTransaction.isTransactionInProgress( jdbcContext, transactionContext );
	}
}
