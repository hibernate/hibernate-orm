/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
