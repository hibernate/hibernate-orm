/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.txn;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.resource.transaction.backend.jta.internal.JtaIsolationDelegate;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@JiraKey("HHH-18961")
public class JtaObtainConnectionExceptionTest {

	@Test
	public void testIt() {
		final JtaIsolationDelegate isolationDelegate = new JtaIsolationDelegate(
				new ObtainConnectionSqlExceptionConnectionAccess(),
				null,
				new TransactionManagerImpl() );

		assertThrowsExactly( HibernateException.class, () ->
				isolationDelegate.delegateWork(
						new AbstractReturningWork<>() {
							@Override
							public Object execute(Connection connection) {
								return null;
							}
						},
						false
				)
		);
	}

	public static class ObtainConnectionSqlExceptionConnectionAccess implements JdbcConnectionAccess {
		@Override
		public Connection obtainConnection() throws SQLException {
			throw new SQLException( "No connection" );
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {

		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}
	}

	public static class TransactionManagerImpl implements TransactionManager {
		@Override
		public void begin() throws NotSupportedException, SystemException {

		}

		@Override
		public void commit()
				throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
		}

		@Override
		public int getStatus() throws SystemException {
			return 0;
		}

		@Override
		public Transaction getTransaction() throws SystemException {
			return null;
		}

		@Override
		public void resume(Transaction transaction)
				throws InvalidTransactionException, IllegalStateException, SystemException {
		}

		@Override
		public void rollback() throws IllegalStateException, SecurityException, SystemException {

		}

		@Override
		public void setRollbackOnly() throws IllegalStateException, SystemException {

		}

		@Override
		public void setTransactionTimeout(int i) throws SystemException {

		}

		@Override
		public Transaction suspend() throws SystemException {
			return null;
		}
	}
}
