/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.orm.test.resource.transaction.jta.JtaPlatformStandardTestingImpl;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = @Setting( name = TRANSACTION_COORDINATOR_STRATEGY, value = "jta" ),
		settingConfigurations = @SettingConfiguration( configurer = TestingJtaBootstrap.class )
)
@DomainModel(annotatedClasses = JtaBeforeCompletionFailureTest.SimpleEntity.class)
@SessionFactory
public class JtaBeforeCompletionFailureTest {
	@BeforeEach
	public void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session ->
			session.persist( newEntity( 1 ) )
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-9888")
	public void testUniqueConstraintViolationDuringManagedFlush(SessionFactoryScope factoryScope) throws Exception {
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction ("CMT" style)
		tm.begin();

		try (Session session = factoryScope.getSessionFactory().openSession()) {
			session.persist( newEntity( 2 ) );
			// complete the transaction ("CMT" style) - this leads to the managed flush
			// which should lead to the UK violation
			try {
				tm.commit();
				fail( "Expecting a failure from JTA commit" );
			}
			catch (RollbackException expected) {
				boolean violationExceptionFound = false;
				Throwable cause = expected;
				while ( cause != null ) {
					if ( cause instanceof JDBCException ) {
						violationExceptionFound = true;
						break;
					}
					cause = cause.getCause();
				}

				if ( !violationExceptionFound ) {
					fail( "Did not find JDBCException in JTA RollbackException chain" );
				}
			}
		}
	}

	private SimpleEntity newEntity(int id) {
		// since "key" is reused, should violate the UK
		return new SimpleEntity( id, "key", "name" );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		public Integer id;
		@Column(unique = true, nullable = false, name = "entity_key")
		public String key;
		public String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String key, String name) {
			this.id = id;
			this.key = key;
			this.name = name;
		}
	}
}
