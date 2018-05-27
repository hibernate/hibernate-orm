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
 * Return a standalone JTA transaction manager for JBoss Transactions
 * Known to work for org.jboss.jbossts:jbossjta:4.9.0.GA
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JBossStandAloneJtaPlatform extends AbstractJtaPlatform {
	public static final String JBOSS_TM_CLASS_NAME = "com.arjuna.ats.jta.TransactionManager";
	public static final String JBOSS_UT_CLASS_NAME = "com.arjuna.ats.jta.UserTransaction";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			final Class jbossTmClass = serviceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( JBOSS_TM_CLASS_NAME );
			return (TransactionManager) jbossTmClass.getMethod( "transactionManager" ).invoke( null );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not obtain JBoss Transactions transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		try {
			final Class jbossUtClass = serviceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( JBOSS_UT_CLASS_NAME );
			return (UserTransaction) jbossUtClass.getMethod( "userTransaction" ).invoke( null );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not obtain JBoss Transactions user transaction instance", e );
		}
	}
}
