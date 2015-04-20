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

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.service.Service;

import static org.hibernate.resource.jdbc.spi.JdbcSessionContext.ConnectionAcquisitionMode;

/**
 * Builder for TransactionCoordinator instances
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorBuilder extends Service {
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner);

	public boolean isJta();

	public ConnectionReleaseMode getDefaultConnectionReleaseMode();

	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode();
}
