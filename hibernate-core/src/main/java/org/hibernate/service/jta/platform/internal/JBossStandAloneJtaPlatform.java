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

import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.lang.reflect.Method;

/**
 * Return a standalone JTA transaction manager for JBoss Transactions
 * Known to work for org.jboss.jbossts:jbossjta:4.11.0.Final
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JBossStandAloneJtaPlatform extends AbstractJtaPlatform {
	private static final String PROPERTY_MANAGER_CLASS_NAME = "com.arjuna.ats.jta.common.jtaPropertyManager";

	private final JtaSynchronizationStrategy synchronizationStrategy = new TransactionManagerBasedSynchronizationStrategy( this );

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			final Class propertyManagerClass = serviceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( PROPERTY_MANAGER_CLASS_NAME );
			final Method getJTAEnvironmentBeanMethod = propertyManagerClass.getMethod( "getJTAEnvironmentBean" );
			final Object jtaEnvironmentBean = getJTAEnvironmentBeanMethod.invoke( null );
			final Method getTransactionManagerMethod = jtaEnvironmentBean.getClass().getMethod( "getTransactionManager" );
			return ( TransactionManager ) getTransactionManagerMethod.invoke( jtaEnvironmentBean );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not obtain JBoss Transactions transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return null;
	}

	@Override
	protected JtaSynchronizationStrategy getSynchronizationStrategy() {
		return synchronizationStrategy;
	}
}
