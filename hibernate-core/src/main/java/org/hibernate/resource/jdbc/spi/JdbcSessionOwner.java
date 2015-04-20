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
package org.hibernate.resource.jdbc.spi;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;

/**
 * @author Steve Ebersole
 */
public interface JdbcSessionOwner {
	/**
	 * Obtain the builder for TransactionCoordinator instances
	 *
	 * @return The TransactionCoordinatorBuilder
	 */
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder();

	public JdbcSessionContext getJdbcSessionContext();

	public JdbcConnectionAccess getJdbcConnectionAccess();

	/**
	 * A after-begin callback from the coordinator to its owner.
	 */
	public void afterTransactionBegin();

	/**
	 * A before-completion callback to the owner.
	 */
	public void beforeTransactionCompletion();

	/**
	 * An after-completion callback to the owner.
	 *
	 * @param successful Was the transaction successful?
	 */
	public void afterTransactionCompletion(boolean successful);
}
