//$Id: $
package org.hibernate.ejb.transaction;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.transaction.CMTTransaction;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.util.JTAHelper;

/**
 * Implements a joinable transaction. Until the transaction is marked for joined, the TM.isTransactionInProgress()
 * must return false
 *
 * @author Emmanuel Bernard
 */
public class JoinableCMTTransaction extends CMTTransaction {
	private JoinStatus status;

	public JoinableCMTTransaction(JDBCContext jdbcContext, TransactionFactory.Context transactionContext) {
		super( jdbcContext, transactionContext );
		//status = JoinStatus.MARKED_FOR_JOINED;
		//tryJoiningTransaction();
	}

	public boolean isTransactionInProgress(
			JDBCContext jdbcContext,
			TransactionFactory.Context transactionContext
	) {
		try {
			return status == JoinStatus.JOINED && isTransactionInProgress(
					transactionContext.getFactory().getTransactionManager().getTransaction()
			);
		}
		catch (SystemException se) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}

	private boolean isTransactionInProgress() {
		try {
			Transaction transaction = transactionContext.getFactory().getTransactionManager().getTransaction();
			return isTransactionInProgress(transaction);
		}
		catch (SystemException se) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}

	private boolean isTransactionInProgress(Transaction tx) throws SystemException {
		return JTAHelper.isTransactionInProgress(tx) && ! JTAHelper.isRollback( tx.getStatus() );
	}

	void tryJoiningTransaction() {
		if ( status == JoinStatus.MARKED_FOR_JOINED ) {
			if ( isTransactionInProgress() ) {
				status = JoinStatus.JOINED;
			}
			else {
				status = JoinStatus.NOT_JOINED;
			}
		}
	}

	@Override
	public void begin() throws HibernateException {
		super.begin();
		status = JoinStatus.JOINED;
	}

	@Override
	public void commit() throws HibernateException {
		/* this method is not supposed to be called
		 * it breaks the flushBeforeCompletion flag optimizeation
		 * regarding flushing skip.
		 * In its current form, it will generate too much flush() calls
		 */
		super.commit();
	}


	public JoinStatus getStatus() {
		return status;
	}

	public void resetStatus() {
		status = JoinStatus.NOT_JOINED;
	}

	public void markForJoined() {
		if ( status != JoinStatus.JOINED ) status = JoinStatus.MARKED_FOR_JOINED;
	}

	public static enum JoinStatus {
		NOT_JOINED,
		MARKED_FOR_JOINED,
		JOINED
	}
}
