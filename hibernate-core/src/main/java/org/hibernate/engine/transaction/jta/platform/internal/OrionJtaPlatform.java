/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

/**
 * {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation for Orion
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class OrionJtaPlatform extends AbstractJtaPlatform {
	public static final String TM_NAME = "java:comp/UserTransaction";
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
