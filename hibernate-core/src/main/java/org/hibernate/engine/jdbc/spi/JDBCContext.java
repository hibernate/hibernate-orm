/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.spi;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.synchronization.CallbackCoordinator;

/**
 * Acts as the SPI for the mediary between "entity-mode related" sessions in terms of
 * their interaction with the JDBC data store.
 *
 * @author Gail Badner
 */
public interface JDBCContext extends Serializable {

	// TODO: Document these methods...

	CallbackCoordinator getJtaSynchronizationCallbackCoordinator();

	void cleanUpJtaSynchronizationCallbackCoordinator();

	SessionFactoryImplementor getFactory();

	ConnectionManager getConnectionManager();

	Connection connection() throws HibernateException;

	boolean registerCallbackIfNecessary();

	boolean registerSynchronizationIfPossible();

	boolean isTransactionInProgress();

	Transaction getTransaction() throws HibernateException;

	void beforeTransactionCompletion(Transaction tx);

	void afterTransactionBegin(Transaction tx);

	void afterTransactionCompletion(boolean success, Transaction tx);

	void afterNontransactionalQuery(boolean success);

	public static interface Context extends TransactionFactory.Context {
		/**
		 * We cannot rely upon this method being called! It is only
		 * called if we are using Hibernate Transaction API.
		 */
		public void afterTransactionBegin(Transaction tx);
		public void beforeTransactionCompletion(Transaction tx);
		public void afterTransactionCompletion(boolean success, Transaction tx);
		public ConnectionReleaseMode getConnectionReleaseMode();
		public boolean isAutoCloseSessionEnabled();
	}
}
