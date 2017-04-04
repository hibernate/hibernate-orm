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

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

/**
 * @author Steve Ebersole
 */
public class BitronixJtaPlatform extends AbstractJtaPlatform {
	public static final String TM_CLASS_NAME = "bitronix.tm.TransactionManagerServices";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			Class transactionManagerServicesClass = serviceRegistry().getService( ClassLoaderService.class ).classForName( TM_CLASS_NAME );
			final Method getTransactionManagerMethod = transactionManagerServicesClass.getMethod( "getTransactionManager" );
			return (TransactionManager) getTransactionManagerMethod.invoke( null );
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not locate Bitronix TransactionManager", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( "java:comp/UserTransaction" );
	}
}
