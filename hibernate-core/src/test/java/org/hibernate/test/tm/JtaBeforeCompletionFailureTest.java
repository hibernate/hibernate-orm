/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tm;

import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.resource.transaction.jta.JtaPlatformStandardTestingImpl;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class JtaBeforeCompletionFailureTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9888" )
	public void testUniqueConstraintViolationDuringManagedFlush() throws Exception {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// set up test data

		Session session = openSession();
		session.getTransaction().begin();
		session.save( newEntity( 1 ) );
		session.getTransaction().commit();
		session.close();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// do the test

		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction ("CMT" style)
		tm.begin();

		session = openSession();

		session.save( newEntity( 2 ) );

		// complete the transaction ("CMT" style) - this leads to the managed flush
		// which should lead to the UK violation
		try {
			tm.commit();
			fail( "Expecting a failure from JTA commit" );
		}
		catch (RollbackException expected) {
			log.info( "Test encountered expected JTA RollbackException; looking for nested JDBCException", expected );
			boolean violationExceptionFound = false;
			Throwable cause = expected;
			while ( cause != null ) {
				if ( cause instanceof JDBCException ) {
					log.info( "Found JDBCException, assuming related to UK violation", cause );
					violationExceptionFound = true;
					break;
				}
				cause = cause.getCause();
			}

			if ( !violationExceptionFound ) {
				fail( "Did not find JDBCException in JTA RollbackException chain" );
			}
		}
		finally {
			if ( !( (SessionImplementor) session ).isClosed() ) {
				session.close();
			}
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// clean up test data

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete SimpleEntity" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	private SimpleEntity newEntity(int id) {
		// since "key" is reused, should violate the UK
		return new SimpleEntity( id, "key", "name" );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		public Integer id;
		@Column(unique = true, name = "entity_key")
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
