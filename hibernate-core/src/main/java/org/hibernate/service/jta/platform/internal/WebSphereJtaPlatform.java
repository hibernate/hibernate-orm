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
package org.hibernate.service.jta.platform.internal;

import java.lang.reflect.Method;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;

/**
 * JTA platform implementation for WebSphere (versions 4, 5.0 and 5.1)
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class WebSphereJtaPlatform extends AbstractJtaPlatform {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, WebSphereJtaPlatform.class.getName());

	public static final String VERSION_5_UT_NAME = "java:comp/UserTransaction";
	public static final String VERSION_4_UT_NAME = "jta/usertransaction";

	private final Class transactionManagerAccessClass;
	private final int webSphereVersion;

	public WebSphereJtaPlatform() {
		try {
			Class clazz;
			int version;
			try {
				clazz = Class.forName( "com.ibm.ws.Transaction.TransactionManagerFactory" );
				version = 5;
                LOG.debug("WebSphere 5.1");
			}
			catch ( Exception e ) {
				try {
					clazz = Class.forName( "com.ibm.ejs.jts.jta.TransactionManagerFactory" );
					version = 5;
                    LOG.debug("WebSphere 5.0");
				}
				catch ( Exception e2 ) {
					clazz = Class.forName( "com.ibm.ejs.jts.jta.JTSXA" );
					version = 4;
                    LOG.debug("WebSphere 4");
				}
			}

			transactionManagerAccessClass = clazz;
			webSphereVersion = version;
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not locate WebSphere TransactionManager access class", e );
		}
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			final Method method = transactionManagerAccessClass.getMethod( "getTransactionManager" );
			return ( TransactionManager ) method.invoke( null );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not obtain WebSphere TransactionManager", e );
		}

	}

	@Override
	protected UserTransaction locateUserTransaction() {
		final String utName = webSphereVersion == 5 ? VERSION_5_UT_NAME : VERSION_4_UT_NAME;
		return (UserTransaction) jndiService().locate( utName );
	}
}
