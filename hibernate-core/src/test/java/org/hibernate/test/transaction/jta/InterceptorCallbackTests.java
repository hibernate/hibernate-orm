/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.transaction.jta;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.JtaTransactionFactory;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class InterceptorCallbackTests extends BaseUnitTestCase {
	@Test
	public void testManagedTransactionCallbacks() {
		SessionFactory sf = new Configuration()
				.setProperty( AvailableSettings.TRANSACTION_STRATEGY, JtaTransactionFactory.SHORT_NAME )
				.buildSessionFactory();

		JournalingInterceptor interceptor = new JournalingInterceptor();

		try {
			Session session = sf.withOptions().interceptor( interceptor ).openSession();
			session.beginTransaction();
			session.getTransaction().commit();
			session.close();
		}
		finally {
			sf.close();
		}

		assertEquals( 1, interceptor.afterStart );
		assertEquals( 1, interceptor.beforeCompletion );
		assertEquals( 1, interceptor.afterCompletion );
	}

	@Test
	public void testTransactionCallbacks() throws Exception {
		SessionFactoryImplementor sf = (SessionFactoryImplementor) new Configuration()
				.setProperty( AvailableSettings.TRANSACTION_STRATEGY, JtaTransactionFactory.SHORT_NAME )
				.setProperty( AvailableSettings.AUTO_CLOSE_SESSION, "true" )
				.buildSessionFactory();

		JournalingInterceptor interceptor = new JournalingInterceptor();

		try {
			JtaPlatform instance = sf.getServiceRegistry().getService( JtaPlatform.class );
			TransactionManager transactionManager = instance.retrieveTransactionManager();

			// start the cmt
			transactionManager.begin();

			Session session = sf.withOptions().interceptor( interceptor ).openSession();

			transactionManager.commit();

			if ( session.isOpen() ) {
				try {
					session.close();
				}
				catch (Exception ignore) {
				}
				fail( "auto-close-session setting did not close session" );
			}
		}
		finally {
			sf.close();
		}

		assertEquals( 0, interceptor.afterStart );
		assertEquals( 1, interceptor.beforeCompletion );
		assertEquals( 1, interceptor.afterCompletion );
	}

	@Test
	public void testTransactionCallbacks2() throws Exception {
		SessionFactoryImplementor sf = (SessionFactoryImplementor) new Configuration()
				.setProperty( AvailableSettings.TRANSACTION_STRATEGY, CMTTransactionFactory.SHORT_NAME )
				.setProperty( AvailableSettings.AUTO_CLOSE_SESSION, "true" )
				.buildSessionFactory();

		JournalingInterceptor interceptor = new JournalingInterceptor();

		try {
			JtaPlatform instance = sf.getServiceRegistry().getService( JtaPlatform.class );
			TransactionManager transactionManager = instance.retrieveTransactionManager();

			// start the cmt
			transactionManager.begin();

			Session session = sf.withOptions().interceptor( interceptor ).openSession();

			transactionManager.commit();

			if ( session.isOpen() ) {
				try {
					session.close();
				}
				catch (Exception ignore) {
				}
				fail( "auto-close-session setting did not close session" );
			}
		}
		finally {
			sf.close();
		}

		assertEquals( 0, interceptor.afterStart );
		assertEquals( 1, interceptor.beforeCompletion );
		assertEquals( 1, interceptor.afterCompletion );
	}

	private static class JournalingInterceptor extends EmptyInterceptor implements Interceptor {
		int afterStart;
		int beforeCompletion;
		int afterCompletion;

		@Override
		public void afterTransactionBegin(Transaction tx) {
			afterStart++;
		}

		@Override
		public void afterTransactionCompletion(Transaction tx) {
			afterCompletion++;
		}

		@Override
		public void beforeTransactionCompletion(Transaction tx) {
			beforeCompletion++;
		}
	}

}
