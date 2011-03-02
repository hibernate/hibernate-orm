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

/**
 * Observer of internal transaction events.
 *
 * @author Steve Ebersole
 */
public interface TransactionObserver {
	/**
	 * Callback for processing the beginning of a transaction.
	 *
	 * Do not rely on this being called as the transaction mat be started in a manner other than through the
	 * {@link org.hibernate.Transaction} API.
	 *
	 * @param transaction The Hibernate transaction
	 */
	public void afterBegin(TransactionImplementor transaction);

	/**
	 * Callback for processing the initial phase of transaction completion.
	 *
	 * @param transaction The Hibernate transaction
	 */
	public void beforeCompletion(TransactionImplementor transaction);

	/**
	 * Callback for processing the last phase of transaction completion.
	 *
	 * @param successful Was the transaction successful?
	 * @param transaction The Hibernate transaction
	 */
	public void afterCompletion(boolean successful, TransactionImplementor transaction);
}

