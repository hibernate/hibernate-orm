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

import java.lang.reflect.Method;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;

/**
 * @author Steve Ebersole
 */
public class JOTMJtaPlatform extends AbstractJtaPlatform {
	public static final String TM_CLASS_NAME = "org.objectweb.jotm.Current";
	public static final String UT_NAME = "java:comp/UserTransaction";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			final Class tmClass = serviceRegistry().getService( ClassLoaderService.class ).classForName( TM_CLASS_NAME );
			final Method getTransactionManagerMethod = tmClass.getMethod( "getTransactionManager" );
			return (TransactionManager) getTransactionManagerMethod.invoke( null, (Object[]) null );
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not obtain JOTM transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( UT_NAME );
	}
}
