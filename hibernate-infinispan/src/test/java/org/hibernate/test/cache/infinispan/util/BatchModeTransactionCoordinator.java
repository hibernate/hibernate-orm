package org.hibernate.test.cache.infinispan.util;

import java.sql.Connection;
import java.sql.SQLException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.resource.transaction.backend.jta.internal.JtaIsolationDelegate;
import org.hibernate.resource.transaction.backend.jta.internal.StatusTranslator;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.infinispan.transaction.tm.DummyTransaction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mocks transaction coordinator when {@link org.hibernate.engine.spi.SharedSessionContractImplementor} is only mocked
 * and {@link org.infinispan.transaction.tm.BatchModeTransactionManager} is used.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class BatchModeTransactionCoordinator implements TransactionCoordinator {
	private BatchModeTransactionManager tm = BatchModeTransactionManager.getInstance();;
	private TransactionDriver transactionDriver = new TransactionDriver() {
		@Override
		public void begin() {
			try {
				tm.begin();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void commit() {
			try {
				tm.commit();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void rollback() {
			try {
				tm.rollback();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public TransactionStatus getStatus() {
			try {
				DummyTransaction transaction = tm.getTransaction();
				return transaction == null ? TransactionStatus.NOT_ACTIVE : StatusTranslator.translate(transaction.getStatus());
			} catch (SystemException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void markRollbackOnly() {
			throw new UnsupportedOperationException();
		}
	};;

	@Override
	public void explicitJoin() {
	}

	@Override
	public boolean isJoined() {
		return true;
	}

	@Override
	public void pulse() {
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		return transactionDriver;
	}

	@Override
	public SynchronizationRegistry getLocalSynchronizations() {
		return new SynchronizationRegistry() {
			@Override
			public void registerSynchronization(Synchronization synchronization) {
				try {
					BatchModeTransactionManager.getInstance().getTransaction().registerSynchronization(synchronization);
				} catch (RollbackException e) {
					throw new RuntimeException(e);
				} catch (SystemException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@Override
	public boolean isActive() {
		try {
			return BatchModeTransactionManager.getInstance().getStatus() == Status.STATUS_ACTIVE;
		} catch (SystemException e) {
			return false;
		}
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		Connection connection = mock(Connection.class);
		JdbcConnectionAccess jdbcConnectionAccess = mock(JdbcConnectionAccess.class);
		try {
			when(jdbcConnectionAccess.obtainConnection()).thenReturn(connection);
		} catch (SQLException e) {
		}
		return new JtaIsolationDelegate(jdbcConnectionAccess, mock(SqlExceptionHelper.class), tm);
	}

	@Override
	public void addObserver(TransactionObserver observer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeObserver(TransactionObserver observer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTimeOut(int seconds) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTimeOut() {
		throw new UnsupportedOperationException();
	}

	public Transaction newTransaction() {
		return new BatchModeTransaction();
	}

	public class BatchModeTransaction implements Transaction {
		@Override
		public void begin() {
		}

		@Override
		public void commit() {
			transactionDriver.commit();
		}

		@Override
		public void rollback() {
			transactionDriver.rollback();
		}

		@Override
		public void setRollbackOnly() {

		}

		@Override
		public boolean getRollbackOnly() {
			return false;
		}

		@Override
		public boolean isActive() {
			return false;
		}

		@Override
		public TransactionStatus getStatus() {
			return transactionDriver.getStatus();
		}

		@Override
		public void registerSynchronization(Synchronization synchronization) throws HibernateException {
			getLocalSynchronizations().registerSynchronization(synchronization);
		}

		@Override
		public void setTimeout(int seconds) {
		}

		@Override
		public int getTimeout() {
			return 0;
		}

		@Override
		public void markRollbackOnly() {
			transactionDriver.markRollbackOnly();
		}
	}
}
