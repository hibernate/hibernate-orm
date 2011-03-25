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
package org.hibernate.engine.transaction.spi;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.service.Service;

/**
 * Contract for transaction creation, as well as providing metadata and contextual information about that creation.
 *
 * @author Steve Ebersole
 */
public interface TransactionFactory<T extends TransactionImplementor> extends Service {
	/**
	 * Construct a transaction instance compatible with this strategy.
	 *
	 * @param coordinator The coordinator for this transaction
	 *
	 * @return The appropriate transaction instance.
	 *
	 * @throws org.hibernate.HibernateException Indicates a problem constructing the transaction.
	 */
	public T createTransaction(TransactionCoordinator coordinator);

	/**
	 * Can the transactions created from this strategy act as the driver?  In other words can the user actually manage
	 * transactions with this strategy?
	 *
	 * @return {@literal true} if the transaction strategy represented by this factory can act as the driver callback;
	 * {@literal false} otherwise.
	 */
	public boolean canBeDriver();

	/**
	 * Should we attempt to register JTA transaction {@link javax.transaction.Synchronization synchronizations}.
	 * <p/>
	 * In other words, is this strategy JTA-based?
	 *
	 * @return {@literal true} if the transaction strategy represented by this factory is compatible with registering
	 * {@link javax.transaction.Synchronization synchronizations}; {@literal false} otherwise.
	 */
	public boolean compatibleWithJtaSynchronization();

	/**
	 * Can the underlying transaction represented by the passed Hibernate {@link TransactionImplementor} be joined?
	 *
	 * @param transactionCoordinator The transaction coordinator
	 * @param transaction The current Hibernate transaction
	 *
	 * @return {@literal true} is the transaction can be joined; {@literal false} otherwise.
	 */
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, T transaction);

	/**
	 * Get the default connection release mode.
	 *
	 * @return The default release mode associated with this strategy
	 */
	public ConnectionReleaseMode getDefaultReleaseMode();

}

