//$Id: CMTTransaction.java 9680 2006-03-22 23:47:31Z epbernard $
package org.hibernate.transaction;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.util.JTAHelper;

/**
 * Implements a basic transaction strategy for CMT transactions. All work is done 
 * in the context of the container managed transaction.
 * @author Gavin King
 */
public class CMTTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(CMTTransaction.class);

	protected final JDBCContext jdbcContext;
	protected final TransactionFactory.Context transactionContext;

	private boolean begun;

	public CMTTransaction(JDBCContext jdbcContext, TransactionFactory.Context transactionContext) {
		this.jdbcContext = jdbcContext;
		this.transactionContext = transactionContext;
	}

	public void begin() throws HibernateException {
		if (begun) {
			return;
		}

		log.debug("begin");
		
		boolean synchronization = jdbcContext.registerSynchronizationIfPossible();

		if ( !synchronization ) {
			throw new TransactionException("Could not register synchronization for container transaction");
		}

		begun = true;
		
		jdbcContext.afterTransactionBegin(this);
	}

	public void commit() throws HibernateException {
		if (!begun) {
			throw new TransactionException("Transaction not successfully started");
		}

		log.debug("commit");

		boolean flush = !transactionContext.isFlushModeNever() &&
		        !transactionContext.isFlushBeforeCompletionEnabled();

		if (flush) {
			transactionContext.managedFlush(); //if an exception occurs during flush, user must call rollback()
		}

		begun = false;

	}

	public void rollback() throws HibernateException {
		if (!begun) {
			throw new TransactionException("Transaction not successfully started");
		}

		log.debug("rollback");

		try {
			getTransaction().setRollbackOnly();
		}
		catch (SystemException se) {
			log.error("Could not set transaction to rollback only", se);
			throw new TransactionException("Could not set transaction to rollback only", se);
		}

		begun = false;

	}

	public javax.transaction.Transaction getTransaction() throws SystemException {
		return transactionContext.getFactory().getTransactionManager().getTransaction();
	}

	public boolean isActive() throws TransactionException {

		if (!begun) return false;

		final int status;
		try {
			status = getTransaction().getStatus();
		}
		catch (SystemException se) {
			log.error("Could not determine transaction status", se);
			throw new TransactionException("Could not determine transaction status: ", se);
		}
		if (status==Status.STATUS_UNKNOWN) {
			throw new TransactionException("Could not determine transaction status");
		}
		else {
			return status==Status.STATUS_ACTIVE;
		}
	}

	public boolean wasRolledBack() throws TransactionException {

		if (!begun) return false;

		final int status;
		try {
			status = getTransaction().getStatus();
		}
		catch (SystemException se) {
			log.error("Could not determine transaction status", se);
			throw new TransactionException("Could not determine transaction status", se);
		}
		if (status==Status.STATUS_UNKNOWN) {
			throw new TransactionException("Could not determine transaction status");
		}
		else {
			return JTAHelper.isRollback(status);
		}
	}

	public boolean wasCommitted() throws TransactionException {

		if ( !begun ) return false;

		final int status;
		try {
			status = getTransaction().getStatus();
		}
		catch (SystemException se) {
			log.error("Could not determine transaction status", se);
			throw new TransactionException("Could not determine transaction status: ", se);
		}
		if (status==Status.STATUS_UNKNOWN) {
			throw new TransactionException("Could not determine transaction status");
		}
		else {
			return status==Status.STATUS_COMMITTED;
		}
	}

	public void registerSynchronization(Synchronization sync) throws HibernateException {
		try {
			getTransaction().registerSynchronization(sync);
		}
		catch (Exception e) {
			throw new TransactionException("Could not register synchronization", e);
		}
	}

	public void setTimeout(int seconds) {
		throw new UnsupportedOperationException("cannot set transaction timeout in CMT");
	}

}
