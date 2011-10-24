/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.transaction;
import java.util.Properties;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;

/**
 * Contract for locating the JTA {@link TransactionManager} on given platform.
 * <p/>
 * NOTE: this contract has expanded over time, and basically is a platform
 * abstraction contract for JTA-related information.
 *
 * @author Gavin King
 */
public interface TransactionManagerLookup {

	/**
	 * Obtain the JTA {@link TransactionManager}.
	 *
	 * @param props The configuration properties.
	 * @return The JTA {@link TransactionManager}.
	 *
	 * @throws HibernateException Indicates problem locating {@link TransactionManager}.
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException;

	/**
	 * Return the JNDI namespace of the JTA
	 * {@link javax.transaction.UserTransaction} for this platform or <tt>null</tt>;
	 * optional operation.
	 *
	 * @return The JNDI namespace where we can locate the
	 * {@link javax.transaction.UserTransaction} for this platform.
	 */
	public String getUserTransactionName();

	/**
	 * Determine an identifier for the given transaction appropriate for use in caching/lookup usages.
	 * <p/>
	 * Generally speaking the transaction itself will be returned here.  This method was added specifically
	 * for use in WebSphere and other unfriendly JEE containers (although WebSphere is still the only known
	 * such brain-dead, sales-driven impl).
	 *
	 * @param transaction The transaction to be identified.
	 * @return An appropropriate identifier
	 */
	public Object getTransactionIdentifier(Transaction transaction);
}

