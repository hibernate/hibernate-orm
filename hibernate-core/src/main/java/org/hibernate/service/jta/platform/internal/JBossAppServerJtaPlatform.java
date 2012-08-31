/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.service.jta.platform.internal;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.service.jndi.JndiException;

/**
 * JtaPlatform definition for JBoss Application Server.
 *
 * @author Steve Ebersole
 */
public class JBossAppServerJtaPlatform extends AbstractJtaPlatform {
	public static final String AS7_TM_NAME = "java:jboss/TransactionManager";
	public static final String AS4_TM_NAME = "java:/TransactionManager";
	public static final String JBOSS__UT_NAME = "java:jboss/UserTransaction";
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
			return (UserTransaction) jndiService().locate( JBOSS__UT_NAME );
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
