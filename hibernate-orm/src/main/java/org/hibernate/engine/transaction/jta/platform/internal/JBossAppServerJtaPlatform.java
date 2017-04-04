/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.jndi.JndiException;

/**
 * JtaPlatform definition for JBoss Application Server.
 *
 * @author Steve Ebersole
 */
public class JBossAppServerJtaPlatform extends AbstractJtaPlatform {
	public static final String AS7_TM_NAME = "java:jboss/TransactionManager";
	public static final String AS4_TM_NAME = "java:/TransactionManager";
	public static final String JBOSS_UT_NAME = "java:jboss/UserTransaction";
	public static final String UT_NAME = "java:comp/UserTransaction";

	@Override
	protected boolean canCacheUserTransactionByDefault() {
		return true;
	}

	@Override
	protected boolean canCacheTransactionManagerByDefault() {
		return true;
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			return (TransactionManager) jndiService().locate( AS7_TM_NAME );
		}
		catch (JndiException jndiException) {
			try {
				return (TransactionManager) jndiService().locate( AS4_TM_NAME );
			}
			catch (JndiException jndiExceptionInner) {
				throw new JndiException( "unable to find transaction manager", jndiException );
			}
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		try {
			return (UserTransaction) jndiService().locate( JBOSS_UT_NAME );
		}
		catch (JndiException jndiException) {
			try {
				return (UserTransaction) jndiService().locate( UT_NAME );
			}
			catch (JndiException jndiExceptionInner) {
				throw new JndiException( "unable to find UserTransaction", jndiException );
			}
		}
	}
}
