/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.resource.transaction;

import org.hibernate.resource.transaction.backend.store.spi.DataStoreTransactionAccess;

/**
 * A builder of TransactionCoordinator instances intended for use in resource-local mode (non-JTA transactions local
 * to the underlying  data store).
 * <p/>
 * NOTE : Ideally I'd love to specialize the {@link #buildTransactionCoordinator(org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner)}
 * method here to only accept TransactionCoordinatorOwner arguments that are specifically
 * {@link org.hibernate.resource.transaction.backend.store.spi.DataStoreTransactionAccess} instances.  Not sure how to
 * best achieve that.  For now we just cast and let the exception happen, but directing the user via the contract
 * would be MUCH preferable.
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorResourceLocalBuilder extends TransactionCoordinatorBuilder {
	/**
	 * Provides the TransactionCoordinator we are building with access to the ResourceLocalTransaction used to control
	 * transactions.
	 * <p/>
	 * An alternative is for the owner passed to {@link #buildTransactionCoordinator} to implement the
	 * ResourceLocalTransactionAccess contract.
	 *
	 * @param dataStoreTransactionAccess Access
	 */
	public void setResourceLocalTransactionAccess(DataStoreTransactionAccess dataStoreTransactionAccess);
}
