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
 * Return a standalone JTA transaction manager for WildFly transaction client
 * Known to work for WildFly 13+
 *
 * @author Scott Marlow
 */
public class WildFlyStandAloneJtaPlatform extends AbstractJtaPlatform {
	public static final String WILDFLY_TM_CLASS_NAME = "org.wildfly.transaction.client.ContextTransactionManager";
	public static final String WILDFLY_UT_CLASS_NAME = "org.wildfly.transaction.client.LocalUserTransaction";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			final Class wildflyTmClass = serviceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( WILDFLY_TM_CLASS_NAME );
			return (TransactionManager) wildflyTmClass.getMethod( "getInstance" ).invoke( null );
		}
		catch (Exception e) {
			throw new JtaPlatformException(
					"Could not obtain WildFly Transaction Client transaction manager instance",
					e
			);
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		try {
			final Class jbossUtClass = serviceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( WILDFLY_UT_CLASS_NAME );
			return (UserTransaction) jbossUtClass.getMethod( "getInstance" ).invoke( null );
		}
		catch (Exception e) {
			throw new JtaPlatformException(
					"Could not obtain WildFly Transaction Client user transaction instance",
					e
			);
		}
	}
}
