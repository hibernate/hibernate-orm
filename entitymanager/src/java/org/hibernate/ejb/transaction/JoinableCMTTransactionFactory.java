//$Id: $
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
	public Transaction createTransaction(JDBCContext jdbcContext, Context transactionContext)
			throws HibernateException {
		return new JoinableCMTTransaction( jdbcContext, transactionContext );
	}

	@Override
	public boolean isTransactionInProgress(
			JDBCContext jdbcContext, Context transactionContext, Transaction transaction
	) {
		if ( transaction == null ) return false; //should not happen though
		JoinableCMTTransaction joinableCMTTransaction = ( (JoinableCMTTransaction) transaction );
		joinableCMTTransaction.tryJoiningTransaction();
		return joinableCMTTransaction.isTransactionInProgress( jdbcContext, transactionContext );
	}
}
