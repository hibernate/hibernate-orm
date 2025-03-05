/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

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
			return (TransactionManager) serviceRegistry()
					.requireService( ClassLoaderService.class )
					.classForName( WILDFLY_TM_CLASS_NAME )
					.getMethod( "getInstance" )
					.invoke( null );
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
			return (UserTransaction) serviceRegistry()
					.requireService( ClassLoaderService.class )
					.classForName( WILDFLY_UT_CLASS_NAME ).getMethod( "getInstance" ).invoke( null );
		}
		catch (Exception e) {
			throw new JtaPlatformException(
					"Could not obtain WildFly Transaction Client user transaction instance",
					e
			);
		}
	}
}
