/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import javax.transaction.Synchronization;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

/**
 * Implementation of the {@link JtaSynchronizationStrategy} contract based on using a
 * {@link javax.transaction.TransactionManager}
 * 
 * @author Steve Ebersole
 */
public class TransactionManagerBasedSynchronizationStrategy implements JtaSynchronizationStrategy {
	private final TransactionManagerAccess transactionManagerAccess;

	public TransactionManagerBasedSynchronizationStrategy(TransactionManagerAccess transactionManagerAccess) {
		this.transactionManagerAccess = transactionManagerAccess;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		try {
			transactionManagerAccess.getTransactionManager().getTransaction().registerSynchronization( synchronization );
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not access JTA Transaction to register synchronization", e );
		}
	}

	@Override
	public boolean canRegisterSynchronization() {
		return JtaStatusHelper.isActive( transactionManagerAccess.getTransactionManager() );
	}
}
