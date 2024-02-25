/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

/**
 * @author Steve Ebersole
 */
public class JOTMJtaPlatform extends AbstractJtaPlatform {
	public static final String TM_CLASS_NAME = "org.objectweb.jotm.Current";
	public static final String UT_NAME = "java:comp/UserTransaction";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			return (TransactionManager) serviceRegistry()
					.requireService( ClassLoaderService.class )
					.classForName( TM_CLASS_NAME )
					.getMethod( "getTransactionManager" )
					.invoke( null, (Object[]) null );
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not obtain JOTM transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( UT_NAME );
	}
}
