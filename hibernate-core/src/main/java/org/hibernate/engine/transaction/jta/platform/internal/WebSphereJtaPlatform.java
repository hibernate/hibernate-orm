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
package org.hibernate.engine.transaction.jta.platform.internal;

import java.lang.reflect.Method;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

import org.jboss.logging.Logger;

/**
 * JTA platform implementation for WebSphere (versions 4, 5.0 and 5.1)
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class WebSphereJtaPlatform extends AbstractJtaPlatform {
	private static final Logger log = Logger.getLogger( WebSphereJtaPlatform.class );

	private final Class transactionManagerAccessClass;
	private final WebSphereEnvironment webSphereEnvironment;

	public WebSphereJtaPlatform() {
		Class tmAccessClass = null;
		WebSphereEnvironment webSphereEnvironment = null;

		for ( WebSphereEnvironment check : WebSphereEnvironment.values() ) {
			try {
				tmAccessClass = Class.forName( check.getTmAccessClassName() );
				webSphereEnvironment = check;
				log.debugf( "WebSphere version : %s", webSphereEnvironment.getWebSphereVersion() );
				break;
			}
			catch ( Exception ignore ) {
				// go on to the next iteration
			}
		}

		if ( webSphereEnvironment == null ) {
			throw new JtaPlatformException( "Could not locate WebSphere TransactionManager access class" );
		}

		this.transactionManagerAccessClass = tmAccessClass;
		this.webSphereEnvironment = webSphereEnvironment;
	}

	public WebSphereJtaPlatform(Class transactionManagerAccessClass, WebSphereEnvironment webSphereEnvironment) {
		this.transactionManagerAccessClass = transactionManagerAccessClass;
		this.webSphereEnvironment = webSphereEnvironment;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected TransactionManager locateTransactionManager() {
		try {
			final Method method = transactionManagerAccessClass.getMethod( "getTransactionManager" );
			return (TransactionManager) method.invoke( null );
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not obtain WebSphere TransactionManager", e );
		}

	}

	@Override
	protected UserTransaction locateUserTransaction() {
		final String utName = webSphereEnvironment.getUtName();
		return (UserTransaction) jndiService().locate( utName );
	}

	public static enum WebSphereEnvironment {
		WS_4_0( "4.x", "com.ibm.ejs.jts.jta.JTSXA", "jta/usertransaction" ),
		WS_5_0( "5.0", "com.ibm.ejs.jts.jta.TransactionManagerFactory", "java:comp/UserTransaction" ),
		WS_5_1( "5.1", "com.ibm.ws.Transaction.TransactionManagerFactory", "java:comp/UserTransaction" )
		;

		private final String webSphereVersion;
		private final String tmAccessClassName;
		private final String utName;

		private WebSphereEnvironment(String webSphereVersion, String tmAccessClassName, String utName) {
			this.webSphereVersion = webSphereVersion;
			this.tmAccessClassName = tmAccessClassName;
			this.utName = utName;
		}

		public String getWebSphereVersion() {
			return webSphereVersion;
		}

		public String getTmAccessClassName() {
			return tmAccessClassName;
		}

		public String getUtName() {
			return utName;
		}
	}
}
