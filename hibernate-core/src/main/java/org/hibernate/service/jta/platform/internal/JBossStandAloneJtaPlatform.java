/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;

/**
 * Return a standalone JTA transaction manager for JBoss Transactions
 * Known to work for org.jboss.jbossts:jbossjta:4.9.0.GA
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JBossStandAloneJtaPlatform extends AbstractJtaPlatform {
	private static final String JBOSS_TM_CLASS_NAME = "com.arjuna.ats.jta.TransactionManager";
	private static final String JBOSS_UT_CLASS_NAME = "com.arjuna.ats.jta.UserTransaction";

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
