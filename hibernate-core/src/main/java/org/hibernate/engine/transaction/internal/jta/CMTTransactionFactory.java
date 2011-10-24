/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.internal.jta;

import javax.transaction.SystemException;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;

/**
 * Factory for Container Managed Transaction (CMT) based transaction facades.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class CMTTransactionFactory  implements TransactionFactory<CMTTransaction> {
	@Override
	public CMTTransaction createTransaction(TransactionCoordinator transactionCoordinator) {
		return new CMTTransaction( transactionCoordinator );
	}

	@Override
	public boolean canBeDriver() {
		return false;
	}

	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return true;
	}

	@Override
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, CMTTransaction transaction) {
		try {
			final int status = transactionCoordinator
					.getTransactionContext()
					.getTransactionEnvironment()
					.getJtaPlatform()
					.getCurrentStatus();
			return JtaStatusHelper.isActive( status );
		}
		catch( SystemException se ) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}

}
