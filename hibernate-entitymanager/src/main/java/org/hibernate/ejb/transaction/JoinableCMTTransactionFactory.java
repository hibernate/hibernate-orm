/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.transaction;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;

/**
 * A transaction is in progress if the underlying JTA tx is in progress and if the Tx is marked as
 * MARKED_FOR_JOINED
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JoinableCMTTransactionFactory implements TransactionFactory<JoinableCMTTransaction> {
	@Override
	public boolean compatibleWithJtaSynchronization() {
		return true;
	}

	@Override
	public boolean canBeDriver() {
		return false;
	}

	@Override
	public JoinableCMTTransaction createTransaction(TransactionCoordinator transactionCoordinator) {
		return new JoinableCMTTransaction( transactionCoordinator );
	}

	@Override
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, JoinableCMTTransaction transaction) {
		return transaction.isJoinable();
	}

	@Override
	public JoinStatus getJoinStatus(TransactionCoordinator transactionCoordinator, JoinableCMTTransaction transaction) {
		return transaction.getJoinStatus();
	}

	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}
}
