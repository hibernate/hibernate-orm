/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.test.tm;

import java.util.Properties;
import javax.transaction.TransactionManager;
import javax.transaction.Transaction;

import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.HibernateException;

/**
 * TransactionManagerLookupImpl implementation
 *
 * @author Steve Ebersole
 */
public class TransactionManagerLookupImpl implements TransactionManagerLookup {
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		return SimpleJtaTransactionManagerImpl.getInstance();
	}

	public String getUserTransactionName() {
		throw new UnsupportedOperationException( "jndi currently not implemented for these tests" );
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}
