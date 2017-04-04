/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

/**
 * {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation for Weblogic
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class WeblogicJtaPlatform extends AbstractJtaPlatform {
	public static final String TM_NAME = "javax.transaction.TransactionManager";
	public static final String UT_NAME = "javax.transaction.UserTransaction";

	@Override
	protected TransactionManager locateTransactionManager() {
		return (TransactionManager) jndiService().locate( TM_NAME );
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( UT_NAME );
	}
}
