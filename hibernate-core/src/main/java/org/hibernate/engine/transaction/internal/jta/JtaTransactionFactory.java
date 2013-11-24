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
import javax.transaction.UserTransaction;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;

/**
 * Factory for {@link JtaTransaction} instances.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Les Hazlewood
 */
public class JtaTransactionFactory implements TransactionFactory<JtaTransaction> {
	public static final String SHORT_NAME = "jta";

	@Override
	public JtaTransaction createTransaction(TransactionCoordinator transactionCoordinator) {
		return new JtaTransaction( transactionCoordinator );
	}

	@Override
	public boolean canBeDriver() {
		return true;
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
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, JtaTransaction transaction) {
		try {
			// Essentially:
			// 1) If we have a local (Hibernate) transaction in progress
			//      and it already has the UserTransaction cached, use that
			//      UserTransaction to determine the status.
			// 2) If a transaction manager has been located, use
			//      that transaction manager to determine the status.
			// 3) Finally, as the last resort, try to lookup the
			//      UserTransaction via JNDI and use that to determine the
			//      status.
			if ( transaction != null ) {
				UserTransaction ut = transaction.getUserTransaction();
				if ( ut != null ) {
					return JtaStatusHelper.isActive( ut );
				}
			}

			final JtaPlatform jtaPlatform = transactionCoordinator
					.getTransactionContext()
					.getTransactionEnvironment()
					.getJtaPlatform();
			if ( jtaPlatform == null ) {
				throw new TransactionException( "Unable to check transaction status" );
			}
			if ( jtaPlatform.retrieveTransactionManager() != null ) {
				return JtaStatusHelper.isActive( jtaPlatform.retrieveTransactionManager().getStatus() );
			}
			else {
				final UserTransaction ut = jtaPlatform.retrieveUserTransaction();
				return ut != null && JtaStatusHelper.isActive( ut );
			}
		}
		catch ( SystemException se ) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}

}
