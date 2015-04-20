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
package org.hibernate.resource.transaction.backend.store.internal;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.backend.store.spi.DataStoreTransactionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.TransactionCoordinatorResourceLocalBuilder;

import static org.hibernate.resource.jdbc.spi.JdbcSessionContext.ConnectionAcquisitionMode;

/**
 * Concrete builder for resource-local TransactionCoordinator instances.
 *
 * @author Steve Ebersole
 */
public class ResourceLocalTransactionCoordinatorBuilderImpl implements TransactionCoordinatorResourceLocalBuilder {
	private DataStoreTransactionAccess providedDataStoreTransactionAccess;

	@Override
	public void setResourceLocalTransactionAccess(DataStoreTransactionAccess dataStoreTransactionAccess) {
		this.providedDataStoreTransactionAccess = dataStoreTransactionAccess;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner) {
		if ( providedDataStoreTransactionAccess != null ) {
			return new ResourceLocalTransactionCoordinatorImpl( this, owner, providedDataStoreTransactionAccess );
		}
		else {
			if ( owner instanceof DataStoreTransactionAccess ) {
				return new ResourceLocalTransactionCoordinatorImpl( this, owner, (DataStoreTransactionAccess) owner );
			}
		}

		throw new HibernateException(
				"Could not determine ResourceLocalTransactionAccess to use in building TransactionCoordinator"
		);
	}

	@Override
	public boolean isJta() {
		return false;
	}

	@Override
	public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return ConnectionReleaseMode.ON_CLOSE;
	}

	@Override
	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return null;
	}
}
