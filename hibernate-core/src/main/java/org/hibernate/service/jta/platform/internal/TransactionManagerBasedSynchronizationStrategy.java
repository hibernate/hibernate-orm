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
package org.hibernate.service.jta.platform.internal;

import javax.transaction.Synchronization;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;

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
