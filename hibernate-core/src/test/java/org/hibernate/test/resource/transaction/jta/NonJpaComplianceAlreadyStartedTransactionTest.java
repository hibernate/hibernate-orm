/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.resource.transaction.jta;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13076")
public class NonJpaComplianceAlreadyStartedTransactionTest extends BaseNonConfigCoreFunctionalTestCase {
	private TransactionManager tm;

	@Override
	protected void addSettings(Map settings) {
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
				s.saveOrUpdate( new TestEntity( "ABC" ) );
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

		public TestEntity(String stringAttribute) {
			this.stringAttribute = stringAttribute;
		}
	}
}
