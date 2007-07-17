package org.hibernate.test.tm;

import org.hibernate.transaction.TransactionFactory;
import org.hibernate.Transaction;
import org.hibernate.HibernateException;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.util.JTAHelper;
import org.hibernate.jdbc.JDBCContext;

import javax.transaction.SystemException;
import javax.transaction.Synchronization;
import java.util.Properties;

/**
 * todo: describe DummyJTAStyleTransationFactory
 *
 * @author Steve Ebersole
 */
public class DummyJTAStyleTransationFactory implements TransactionFactory {
	public Transaction createTransaction(
			JDBCContext jdbcContext,
			Context context) throws HibernateException {
		return new DummyTransactionAdapter();
	}

	public void configure(Properties props) throws HibernateException {
	}

	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
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
			return JTAHelper.isTransactionInProgress( DummyTransactionManager.INSTANCE.getCurrent() )
			       && ! JTAHelper.isMarkedForRollback( DummyTransactionManager.INSTANCE.getCurrent() );
		}
		catch( SystemException e ) {
			throw new HibernateException( e );
		}
	}

	private static class DummyTransactionAdapter implements Transaction {

		private boolean started;
		private boolean committed;
		private boolean rolledback;

		public void begin() throws HibernateException {
			started = true;
			committed = false;
			rolledback = false;
			try {
				DummyTransactionManager.INSTANCE.begin();
			}
			catch( Throwable t ) {
				throw new HibernateException( "error on begin()", t );
			}
		}

		public void commit() throws HibernateException {
			if ( !started ) {
				throw new HibernateException( "not yet started!" );
			}
			started = false;
			rolledback = false;
			committed = false;
			try {
				DummyTransactionManager.INSTANCE.commit();
				committed = true;
			}
			catch( Throwable t ) {
				throw new HibernateException( "error on commit()", t );
			}
		}

		public void rollback() throws HibernateException {
			if ( !started ) {
				throw new HibernateException( "not yet started!" );
			}
			started = false;
			rolledback = false;
			committed = false;
			try {
				DummyTransactionManager.INSTANCE.rollback();
				rolledback = true;
			}
			catch( Throwable t ) {
				throw new HibernateException( "error on rollback()", t );
			}
		}

		public boolean wasRolledBack() throws HibernateException {
			return rolledback;
		}

		public boolean wasCommitted() throws HibernateException {
			return committed;
		}

		public boolean isActive() throws HibernateException {
			return started;
		}

		public void registerSynchronization(Synchronization synchronization) throws HibernateException {
			try {
				DummyTransactionManager.INSTANCE.getCurrent().registerSynchronization( synchronization );
			}
			catch( Throwable t ) {
				throw new HibernateException( "error on registerSynchronization()", t );
			}
		}

		public void setTimeout(int seconds) {
			// ignore...
		}
	}

	public static void setup(Configuration cfg) {
		cfg.setProperty( Environment.CONNECTION_PROVIDER, DummyConnectionProvider.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_MANAGER_STRATEGY, DummyTransactionManagerLookup.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, DummyJTAStyleTransationFactory.class.getName() );
		cfg.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "true" );
	}
}
