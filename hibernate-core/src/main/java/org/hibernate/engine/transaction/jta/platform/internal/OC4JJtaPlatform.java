/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

/**
 * {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation for the OC4J (Oracle) AS.
 *
 * @author Stijn Janssens
 * @author Steve Ebersole
 */
public class OC4JJtaPlatform extends AbstractJtaPlatform {
	public static final String TM_NAME = "java:comp/pm/TransactionManager";
	public static final String UT_NAME = "java:comp/UserTransaction";

	@Override
	protected TransactionManager locateTransactionManager() {
		return (TransactionManager) jndiService().locate( TM_NAME );
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( UT_NAME );
	}
}
