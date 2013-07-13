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
package org.hibernate.engine.transaction.internal.jdbc;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;

/**
 * Factory for {@link org.hibernate.engine.transaction.internal.jdbc.JdbcTransaction} instances.
 *
 * @author Anton van Straaten
 * @author Steve Ebersole
 */
public final class JdbcTransactionFactory implements TransactionFactory<JdbcTransaction> {
	public static final String SHORT_NAME = "jdbc";

	@Override
	public JdbcTransaction createTransaction(TransactionCoordinator transactionCoordinator) {
		return new JdbcTransaction( transactionCoordinator );
	}

	@Override
	public boolean canBeDriver() {
		return true;
	}

	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.ON_CLOSE;
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return false;
	}

	@Override
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, JdbcTransaction transaction) {
		return false;
	}
}
