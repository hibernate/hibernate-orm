/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.lang.reflect.Method;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

/**
 * JTA platform implementation for JOnAS
 *
 * @author Steve Ebersole
 */
public class JOnASJtaPlatform extends AbstractJtaPlatform {
	public static final String UT_NAME = "java:comp/UserTransaction";
	public static final String TM_CLASS_NAME = "org.objectweb.jonas_tm.Current";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			final Class clazz = Class.forName( TM_CLASS_NAME );
			final Method getTransactionManagerMethod = clazz.getMethod( "getTransactionManager" );
			return (TransactionManager) getTransactionManagerMethod.invoke( null );
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not obtain JOnAS transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( UT_NAME );
	}
}
