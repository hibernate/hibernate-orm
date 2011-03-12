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

import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.Transaction;

import org.hibernate.HibernateException;
import org.hibernate.util.NamingHelper;

/**
 * Template implementation of {@link TransactionManagerLookup} where the
 * underlying {@link TransactionManager} is available via JNDI lookup at the
 * specified location - {@link #getName}.
 *
 * @author Gavin King
 */
public abstract class JNDITransactionManagerLookup implements TransactionManagerLookup {

	/**
	 * Get the JNDI namespace under wich we can locate the {@link TransactionManager}.
	 *
	 * @return The {@link TransactionManager} JNDI namespace
	 */
	protected abstract String getName();

	/**
	 * {@inheritDoc}
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			return (TransactionManager) NamingHelper.getInitialContext(props).lookup( getName() );
		}
		catch (NamingException ne) {
			throw new HibernateException( "Could not locate TransactionManager", ne );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getTransactionIdentifier(Transaction transaction) {
		// for sane JEE/JTA containers, the transaction itself functions as its identifier...
		return transaction;
	}
}






