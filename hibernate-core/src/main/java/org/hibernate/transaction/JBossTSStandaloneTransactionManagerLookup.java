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
 *
 */
package org.hibernate.transaction;

import java.lang.reflect.Method;
import java.util.Properties;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;

/**
 * Return a standalone JTA transaction manager for JBoss Transactions
 * Known to work for org.jboss.jbossts:jbossjta:4.11.0.Final
 *
 * @author Emmanuel Bernard
 */
public class JBossTSStandaloneTransactionManagerLookup implements TransactionManagerLookup {

	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			//Call jtaPropertyManager.getJTAEnvironmentBean().getTransactionManager();

			//improper camel case name for the class
			Class<?> propertyManager = Class.forName( "com.arjuna.ats.jta.common.jtaPropertyManager" );
			final Method getJTAEnvironmentBean = propertyManager.getMethod( "getJTAEnvironmentBean" );
			//static method
			final Object jtaEnvironmentBean = getJTAEnvironmentBean.invoke( null );
			final Method getTransactionManager = jtaEnvironmentBean.getClass().getMethod( "getTransactionManager" );
			return ( TransactionManager ) getTransactionManager.invoke( jtaEnvironmentBean );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not obtain JBoss Transactions transaction manager instance", e );
		}
	}

	public String getUserTransactionName() {
		return null;
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}
