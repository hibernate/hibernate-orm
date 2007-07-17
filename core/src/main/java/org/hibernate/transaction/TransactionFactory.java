//$Id: TransactionFactory.java 9595 2006-03-10 18:14:21Z steve.ebersole@jboss.com $
package org.hibernate.transaction;

import java.util.Properties;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * An abstract factory for <tt>Transaction</tt> instances. Concrete implementations
 * are specified by <tt>hibernate.transaction.factory_class</tt>.<br>
 * <br>
 * Implementors must be threadsafe and should declare a public default constructor.
 * @see Transaction
 *
 * @author Anton van Straaten, Gavin King
 */
public interface TransactionFactory {

	public static interface Context {
		public SessionFactoryImplementor getFactory();
//		public boolean isOpen();
		public boolean isClosed();

		public boolean isFlushModeNever();
		public boolean isFlushBeforeCompletionEnabled();
		public void managedFlush();

		public boolean shouldAutoClose();
		public void managedClose();
	}

	/**
	 * Begin a transaction and return the associated <tt>Transaction</tt> instance.
	 *
	 * @param jdbcContext  The jdbc context to which the transaction belongs
	 * @param context The contract regarding the context in which this transaction will operate.
	 * @return Transaction
	 * @throws HibernateException
	 */
	public Transaction createTransaction(JDBCContext jdbcContext, Context context) throws HibernateException;

	/**
	 * Configure from the given properties.
	 * @param props
	 * @throws HibernateException
	 */
	public void configure(Properties props) throws HibernateException;
	
	/**
	 * Get the default connection release mode
	 */
	public ConnectionReleaseMode getDefaultReleaseMode();
	
	/**
	 * Do we require access to the JTA TransactionManager for
	 * this strategy?
	 */
	public boolean isTransactionManagerRequired();

	/**
	 * Are all transaction callbacks local to Hibernate Transactions?
	 * Or can the callbacks originate from some other source (e.g.
	 * a JTA Synchronization).
	 *
	 * @return true if callbacks only ever originate from
	 * the Hibernate {@link Transaction}; false otherwise.
	 */
	public boolean areCallbacksLocalToHibernateTransactions();

	/**
	 * Determine whether an underlying transaction is in progress.
	 * <p/>
	 * Mainly this is used in determining whether to register a
	 * synchronization as well as whether or not to circumvent
	 * auto flushing outside transactions.
	 *
	 * @param jdbcContext
	 * @param transactionContext
	 * @param transaction
	 * @return true if an underlying transaction is know to be in effect.
	 */
	public boolean isTransactionInProgress(JDBCContext jdbcContext, Context transactionContext, Transaction transaction);
}
