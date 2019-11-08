/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.connections;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.batch.internal.AbstractBatchImpl;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertFalse;

@TestForIssue( jiraKey = "HHH-13307" )
@RequiresDialect(H2Dialect.class)
public class JdbcBatchingAgressiveReleaseTest extends BaseNonConfigCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, AbstractBatchImpl.class.getName() )
	);

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000010" );


	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class.getName() );
		settings.put( Environment.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT.toString() );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "500" );
	}

	@Test
	public void testJdbcBatching()  throws Throwable {
		triggerable.reset();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session session = openSession();
		// The following 2 entity inserts will be batched.
		session.persist( new Person( 1, "Jane" ) );
		session.persist( new Person( 2, "Sally" ) );
		// The following entity has an IDENTITY ID, which cannot be batched.
		// As a result the existing batch is forced to execute before the Thing can be
		// inserted.
		session.persist( new Thing( "it" ) );
		session.close();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertFalse( triggerable.wasTriggered() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class, Thing.class };
	}

	@Entity( name = "Person")
	public static class Person {

		@Id
		private int id;
		private String name;

		public Person() {
		}

		public Person(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "Thing")
	public static class Thing {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;
		private String name;

		public Thing() {
		}

		public Thing(String name) {
			this.id = id;
			this.name = name;
		}
	}
}
