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

import javax.transaction.TransactionManager;
import javax.transaction.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;

/**
 * {@link TransactionManagerLookup} strategy for WebSphere (versions 4, 5.0 and 5.1)
 *
 * @author Gavin King
 */
public class WebSphereTransactionManagerLookup implements TransactionManagerLookup {

	private static final Logger log = LoggerFactory.getLogger(WebSphereTransactionManagerLookup.class);
	private final int wsVersion;
	private final Class tmfClass;
	
	/**
	 * Constructs a new WebSphereTransactionManagerLookup.
	 */
	public WebSphereTransactionManagerLookup() {
		try {
			Class clazz;
			int version;
			try {
				clazz = Class.forName( "com.ibm.ws.Transaction.TransactionManagerFactory" );
				version = 5;
				log.info( "WebSphere 5.1" );
			}
			catch ( Exception e ) {
				try {
					clazz = Class.forName( "com.ibm.ejs.jts.jta.TransactionManagerFactory" );
					version = 5;
					log.info( "WebSphere 5.0" );
				} 
				catch ( Exception e2 ) {
					clazz = Class.forName( "com.ibm.ejs.jts.jta.JTSXA" );
					version = 4;
					log.info( "WebSphere 4" );
				}
			}

			tmfClass = clazz;
			wsVersion = version;
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not obtain WebSphere TransactionManagerFactory instance", e );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			return ( TransactionManager ) tmfClass.getMethod( "getTransactionManager", null ).invoke( null, null );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not obtain WebSphere TransactionManager", e );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUserTransactionName() {
		return wsVersion == 5
				? "java:comp/UserTransaction"
				: "jta/usertransaction";
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}