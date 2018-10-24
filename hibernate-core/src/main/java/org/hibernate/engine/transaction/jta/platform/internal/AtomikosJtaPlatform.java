/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

/**
 * @author Vlad Mihalcea
 */
public class AtomikosJtaPlatform extends AbstractJtaPlatform {
	public static final String TM_CLASS_NAME = "com.atomikos.icatch.jta.UserTransactionManager";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			Class transactionManagerClass = serviceRegistry().getService( ClassLoaderService.class ).classForName( TM_CLASS_NAME );
			return  (TransactionManager) transactionManagerClass.newInstance();
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not instantiate Atomikos TransactionManager", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( "java:comp/UserTransaction" );
	}
}
