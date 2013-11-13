/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.common;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class TransactionContextImpl implements TransactionContext {
	private final TransactionEnvironment transactionEnvironment;
	private final JdbcConnectionAccess jdbcConnectionAccess;

	public TransactionContextImpl(TransactionEnvironment transactionEnvironment, JdbcConnectionAccess jdbcConnectionAccess) {
		this.transactionEnvironment = transactionEnvironment;
		this.jdbcConnectionAccess = jdbcConnectionAccess;
	}

	public TransactionContextImpl(TransactionEnvironment transactionEnvironment, ServiceRegistry serviceRegistry) {
		this( transactionEnvironment, new JdbcConnectionAccessImpl( serviceRegistry ) );
	}

	public TransactionContextImpl(TransactionEnvironment transactionEnvironment) {
		this( transactionEnvironment, new JdbcConnectionAccessImpl( transactionEnvironment.getJdbcServices().getConnectionProvider() ) );
	}

	@Override
	public TransactionEnvironment getTransactionEnvironment() {
		return transactionEnvironment;
	}

	@Override
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return transactionEnvironment.getTransactionFactory().getDefaultReleaseMode();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		return jdbcConnectionAccess;
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return false;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public boolean isFlushModeNever() {
		return false;
	}

	@Override
	public boolean isFlushBeforeCompletionEnabled() {
		return true;
	}

	@Override
	public void managedFlush() {
	}

	@Override
	public boolean shouldAutoClose() {
		return false;
	}

	@Override
	public void managedClose() {
	}

	@Override
	public void afterTransactionBegin(TransactionImplementor hibernateTransaction) {
	}

	@Override
	public void beforeTransactionCompletion(TransactionImplementor hibernateTransaction) {
	}

	@Override
	public void afterTransactionCompletion(TransactionImplementor hibernateTransaction, boolean successful) {
	}

	@Override
	public String onPrepareStatement(String sql) {
		return sql;
	}

	@Override
	public void startPrepareStatement() {
	}

	@Override
	public void endPrepareStatement() {
	}

	@Override
	public void startStatementExecution() {
	}

	@Override
	public void endStatementExecution() {
	}

	@Override
	public void startBatchExecution() {
	}

	@Override
	public void endBatchExecution() {
	}
}
