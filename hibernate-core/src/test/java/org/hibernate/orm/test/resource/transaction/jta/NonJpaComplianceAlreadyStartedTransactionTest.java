/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jta;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

/**
 * @author Andrea Boriero
 */
@JiraKey("HHH-13076")
public class NonJpaComplianceAlreadyStartedTransactionTest extends BaseNonConfigCoreFunctionalTestCase {
	private TransactionManager tm;

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Before
	public void setUp() {
		tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
	}

	@Test
	public void noIllegalStateExceptionShouldBeThrownWhenBeginTxIsCalledWithAnAlreadyActiveTx() throws Exception {
		tm.begin();
		try (Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			try {
				s.persist( new TestEntity( "ABC" ) );
				tx.commit();
			}
			catch (Exception e) {
				if ( tx.isActive() ) {
					tx.rollback();
				}
				throw e;
			}
		}
		try {
			tm.commit();
		}
		catch (Exception e) {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.rollback();
			}
			throw e;
		}
	}


	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String stringAttribute;

		public TestEntity() {
		}

		public TestEntity(String stringAttribute) {
			this.stringAttribute = stringAttribute;
		}
	}
}
