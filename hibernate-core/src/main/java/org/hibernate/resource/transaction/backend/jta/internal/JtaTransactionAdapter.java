/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Adapter for abstracting the physical means of interacting with JTA transactions.
 * <p/>
 * JTA transactions can concretely be interacted with through {@link javax.transaction.UserTransaction}
 * or {@link javax.transaction.Transaction} depending on environment and situation.  This adapter hides
 * this difference.
 *
 * @author Steve Ebersole
 */
public interface JtaTransactionAdapter {
	/**
	 * Call begin on the underlying transaction object
	 */
	public void begin();

	/**
	 * Call commit on the underlying transaction object
	 */
	public void commit();

	/**
	 * Call rollback on the underlying transaction object
	 */
	public void rollback();

	public TransactionStatus getStatus();

	public void markRollbackOnly();

	public void setTimeOut(int seconds);
}
