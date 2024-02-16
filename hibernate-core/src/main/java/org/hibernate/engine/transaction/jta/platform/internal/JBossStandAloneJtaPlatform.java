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
 * Return a standalone JTA transaction manager for JBoss (Arjuna) Transactions or WildFly transaction client
 * Known to work for org.jboss.jbossts:jbossjta:4.9.0.GA as well as WildFly 11+
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JBossStandAloneJtaPlatform extends AbstractJtaPlatform {

	public static final String JBOSS_TM_CLASS_NAME = "com.arjuna.ats.jta.TransactionManager";
	public static final String JBOSS_UT_CLASS_NAME = "com.arjuna.ats.jta.UserTransaction";

	private static final WildFlyStandAloneJtaPlatform wildflyBasedAlternative = new WildFlyStandAloneJtaPlatform();

	@Override
	protected TransactionManager locateTransactionManager() {
		//Try WildFly first as it's the "new generation":
		try {
			return wildflyBasedAlternative.locateTransactionManager();
		}
		catch ( Exception ignore) {
			// ignore and look for Arjuna class
		}

		try {
			return (TransactionManager) serviceRegistry().requireService( ClassLoaderService.class )
					.classForName( JBOSS_TM_CLASS_NAME )
					.getMethod( "transactionManager" )
					.invoke( null );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not obtain JBoss Transactions transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		//Try WildFly first as it's the "new generation":
		try {
			return wildflyBasedAlternative.locateUserTransaction();
		}
		catch ( Exception ignore) {
			// ignore and look for Arjuna class
		}

		try {
			return (UserTransaction) serviceRegistry()
					.requireService( ClassLoaderService.class )
					.classForName( JBOSS_UT_CLASS_NAME )
					.getMethod( "userTransaction" )
					.invoke( null );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not obtain JBoss Transactions user transaction instance", e );
		}
	}
}
