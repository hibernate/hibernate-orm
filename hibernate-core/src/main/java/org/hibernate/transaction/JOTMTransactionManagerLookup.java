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
package org.hibernate.transaction;

import java.lang.reflect.Method;
import java.util.Properties;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.hibernate.HibernateException;

/**
 * {@link TransactionManagerLookup} strategy for JOTM
 *
 * @author Low Heng Sin
 */
@SuppressWarnings( {"UnusedDeclaration"})
public class JOTMTransactionManagerLookup implements TransactionManagerLookup {

	private static final String TM_CLASS_NAME = "org.objectweb.jotm.Current";

	/**
	 * {@inheritDoc}
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			final Class clazz = Class.forName( TM_CLASS_NAME );
			final Method method = clazz.getMethod( "getTransactionManager", (Class[]) null );
			return (TransactionManager) method.invoke( null, (Object[]) null );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not obtain JOTM transaction manager instance", e );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}






