/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tm;

import java.util.Arrays;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import javax.transaction.Status;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.CustomParameterized;
import org.hibernate.test.exceptionhandling.BaseJpaOrNativeBootstrapFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@TestForIssue( jiraKey = "HHH-13326")
@RunWith(CustomParameterized.class)
public class InterceptorTransactionTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {

	public enum JpaComplianceTransactionSetting { DEFAULT, TRUE, FALSE }
	public enum JtaAllowTransactionAccessSetting {
		DEFAULT {
			@Override
			public boolean allowTransactionAccess(
					SessionFactory sessionFactory,
					JpaComplianceTransactionSetting jpaComplianceTransactionSetting) {
				return !sessionFactory.getSessionFactoryOptions().isJpaBootstrap() ||
						jpaComplianceTransactionSetting != JpaComplianceTransactionSetting.TRUE;
			}
		},
		TRUE {
			@Override
			public boolean allowTransactionAccess(
					SessionFactory sessionFactory,
					JpaComplianceTransactionSetting jpaComplianceTransactionSetting) {
				return true;
			}
		},
		FALSE {
			@Override
			public boolean allowTransactionAccess(
					SessionFactory sessionFactory,
					JpaComplianceTransactionSetting jpaComplianceTransactionSetting) {
				// setting is ignored if jpaComplianceTransactionSetting != JpaComplianceTransactionSetting.TRUE
				return jpaComplianceTransactionSetting != JpaComplianceTransactionSetting.TRUE;
			}
		};

		public abstract boolean allowTransactionAccess(
				SessionFactory sessionFactory,
				JpaComplianceTransactionSetting jpaComplianceTransactionSetting
		);
	};

	@Parameterized.Parameters(name = "Bootstrap={0}, JpaComplianceTransactionSetting={1}, JtaAllowTransactionAccessSetting={2}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList( new Object[][] {
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.DEFAULT, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.FALSE },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.FALSE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.DEFAULT, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.FALSE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.FALSE },
		} );
	}

	private final JpaComplianceTransactionSetting jpaComplianceTransactionSetting;
	private final JtaAllowTransactionAccessSetting jtaAllowTransactionAccessSetting;

	public InterceptorTransactionTest(
			BootstrapMethod bootstrapMethod,
			JpaComplianceTransactionSetting jpaComplianceTransactionSetting,
			JtaAllowTransactionAccessSetting jtaAllowTransactionAccessSetting) {
		super( bootstrapMethod );
		this.jpaComplianceTransactionSetting = jpaComplianceTransactionSetting;
		this.jtaAllowTransactionAccessSetting = jtaAllowTransactionAccessSetting;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	protected void configure(Map<Object, Object> properties) {
		super.configure( properties );
		TestingJtaBootstrap.prepare( properties );
		properties.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );

		switch ( jpaComplianceTransactionSetting ) {
			case DEFAULT:
				// Keep the default (false)
				break;
			case TRUE:
				properties.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
				break;
			case FALSE:
				properties.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "false" );
				break;

		}
		switch ( jtaAllowTransactionAccessSetting ) {
			case DEFAULT:
				// Keep the default (true native bootstrap; false if jpa bootstrap)
				break;
			case TRUE:
				properties.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
				break;
			case FALSE:
				properties.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "false" );
				break;
		}
	}

	@Test
	public void testHibernateTransactionApi() throws Exception {

		final TransactionInterceptor interceptor = new TransactionInterceptor();

		Session session = sessionFactory().withOptions().interceptor( interceptor ).openSession();

		try {
			session.getTransaction().begin();
			if ( !jtaAllowTransactionAccessSetting.allowTransactionAccess(
					sessionFactory(),
					jpaComplianceTransactionSetting
			) ) {
				fail( "IllegalStateException should have been thrown." );
			}
		}
		catch (IllegalStateException ex) {
			if ( TestingJtaPlatformImpl.INSTANCE.getTransactionManager().getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().setRollbackOnly();
			}
			session.close();
			assertEquals( JpaComplianceTransactionSetting.TRUE, jpaComplianceTransactionSetting );
			return;  // EARLY RETURN
		}

		// Interceptor#afterTransactionBegin is never called when using JTA
		assertFalse( interceptor.afterTransactionBeginMethodCalled );
		assertNull( interceptor.afterTransactionBeginAssertionPassed );
		assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
		assertNull( interceptor.afterTransactionCompletionAssertionPassed );

		SimpleEntity entity = new SimpleEntity( "Hello World" );
		session.save( entity );

		interceptor.reset();

		session.getTransaction().commit();

		assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
		assertTrue( interceptor.afterTransactionCompletionMethodCalled );
		assertEquals( true, interceptor.beforeTransactionCompletionAssertionPassed );
		assertEquals( true, interceptor.afterTransactionCompletionAssertionPassed );
		assertNull( interceptor.afterTransactionBeginAssertionPassed );

		session.close();
	}

	@Test
	public void testJtaApi() throws Exception {

		final TransactionInterceptor interceptor = new TransactionInterceptor();

		Session session = sessionFactory().withOptions().interceptor( interceptor ).openSession();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		// Interceptor#afterTransactionBegin is never called when using JTA
		assertFalse( interceptor.afterTransactionBeginMethodCalled );
		assertNull( interceptor.afterTransactionBeginAssertionPassed );
		assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
		assertNull( interceptor.afterTransactionCompletionAssertionPassed );

		SimpleEntity entity = new SimpleEntity( "Hello World" );
		session.save( entity );

		interceptor.reset();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
		assertTrue( interceptor.afterTransactionCompletionMethodCalled );
		if ( jtaAllowTransactionAccessSetting.allowTransactionAccess(
				sessionFactory(),
				jpaComplianceTransactionSetting
		) ) {
			assertEquals( true, interceptor.beforeTransactionCompletionAssertionPassed );
			assertEquals( true, interceptor.afterTransactionCompletionAssertionPassed );
		}
		else {
			assertEquals( null, interceptor.beforeTransactionCompletionAssertionPassed );
			assertEquals( null, interceptor.afterTransactionCompletionAssertionPassed );
		}

		assertNull( interceptor.afterTransactionBeginAssertionPassed );

		session.close();
	}

	@Test
	public void testJtaApiWithSharedTransactionCoordinator() throws Exception {

		final TransactionInterceptor interceptor = new TransactionInterceptor();

		Session originalSession = openSession();

		Session session =  originalSession.sessionWithOptions().connection().interceptor( interceptor ).openSession();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		// Interceptor#afterTransactionBegin is never called when using JTA
		assertFalse( interceptor.afterTransactionBeginMethodCalled );
		assertNull( interceptor.afterTransactionBeginAssertionPassed );
		assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
		assertNull( interceptor.afterTransactionCompletionAssertionPassed );

		SimpleEntity entity = new SimpleEntity( "Hello World" );
		session.save( entity );

		interceptor.reset();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
		assertTrue( interceptor.afterTransactionCompletionMethodCalled );
		if ( jtaAllowTransactionAccessSetting.allowTransactionAccess(
				sessionFactory(),
				jpaComplianceTransactionSetting
		) ) {
			assertEquals( true, interceptor.beforeTransactionCompletionAssertionPassed );
			assertEquals( true, interceptor.afterTransactionCompletionAssertionPassed );
		}
		else {
			assertEquals( null, interceptor.beforeTransactionCompletionAssertionPassed );
			assertEquals( null, interceptor.afterTransactionCompletionAssertionPassed );
		}

		assertNull( interceptor.afterTransactionBeginAssertionPassed );

		session.close();

		originalSession.close();
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		SimpleEntity() {

		}

		SimpleEntity(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private class TransactionInterceptor extends EmptyInterceptor {
		private boolean afterTransactionBeginMethodCalled;
		private Boolean afterTransactionBeginAssertionPassed;


		private boolean beforeTransactionCompletionMethodCalled;
		private Boolean beforeTransactionCompletionAssertionPassed;

		private boolean afterTransactionCompletionMethodCalled;
		private Boolean afterTransactionCompletionAssertionPassed;

		public void reset() {
			afterTransactionBeginMethodCalled = false;
			afterTransactionBeginAssertionPassed = null;
			beforeTransactionCompletionMethodCalled = false;
			beforeTransactionCompletionAssertionPassed = null;
			afterTransactionCompletionMethodCalled = false;
			afterTransactionCompletionAssertionPassed = null;
		}

		@Override
		public void afterTransactionBegin(org.hibernate.Transaction tx) {
			afterTransactionBeginMethodCalled = true;
			if ( tx != null ) {
				afterTransactionBeginAssertionPassed = false;
				assertEquals( TransactionStatus.ACTIVE, tx.getStatus() );
				afterTransactionBeginAssertionPassed = true;
			}
		}
		@Override
		public void beforeTransactionCompletion(org.hibernate.Transaction tx) {
			beforeTransactionCompletionMethodCalled = true;
			if ( tx != null ) {
				beforeTransactionCompletionAssertionPassed = false;
				assertEquals( TransactionStatus.ACTIVE, tx.getStatus() );
				beforeTransactionCompletionAssertionPassed = true;
			}
		}
		@Override
		public void afterTransactionCompletion(org.hibernate.Transaction tx) {
			afterTransactionCompletionMethodCalled = true;
			if ( tx != null ) {
				afterTransactionCompletionAssertionPassed = false;
				assertEquals( TransactionStatus.COMMITTED, tx.getStatus() );
				afterTransactionCompletionAssertionPassed = true;
			}
		}
	};
}
