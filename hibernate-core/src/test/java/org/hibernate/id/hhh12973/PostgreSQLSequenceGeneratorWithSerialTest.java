/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.hhh12973;

import java.io.StringReader;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12973")
@RequiresDialect(PostgreSQL82Dialect.class)
public class PostgreSQLSequenceGeneratorWithSerialTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger( CoreMessageLogger.class,
																								   SequenceStyleGenerator.class
																										   .getName()
	) );

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000497:" );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ApplicationConfiguration.class,
		};
	}

	private static final String DROP_SEQUENCE = "DROP SEQUENCE IF EXISTS application_configurations_id_seq";
	private static final String DROP_TABLE = "DROP TABLE IF EXISTS application_configurations CASCADE";
	private static final String CREATE_TABLE = "CREATE TABLE application_configurations (id BIGSERIAL NOT NULL PRIMARY KEY)";

	@Override
	protected void addMappings(Map settings) {
		triggerable.reset();
		assertFalse( triggerable.wasTriggered() );

		//For this test, we need to make sure the DB is created prior to bootstrapping Hibernate
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.build();

		SessionFactory sessionFactory = null;

		try {
			Configuration config = new Configuration();
			sessionFactory = config.buildSessionFactory( ssr );

			try(Session session = sessionFactory.openSession()) {
				session.doWork( connection -> {
					try(Statement statement = connection.createStatement()) {
						statement.execute( DROP_TABLE );
						statement.execute( DROP_SEQUENCE );
						statement.execute( CREATE_TABLE );
					}
				} );
			}
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			ssr.close();
		}

		settings.put( AvailableSettings.HBM2DDL_DROP_SCRIPT_SOURCE, new StringReader(
				DROP_TABLE + ";" + DROP_SEQUENCE
		) );
		settings.put( AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, SequenceMismatchStrategy.FIX);
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	public void test() {

		final AtomicLong id = new AtomicLong();

		final int ITERATIONS = 51;

		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 1; i <= ITERATIONS; i++ ) {
				ApplicationConfiguration model = new ApplicationConfiguration();

				entityManager.persist( model );

				id.set( model.getId() );
			}
		} );

		assertEquals( ITERATIONS, id.get() );
	}

	@Entity
	@Table(name = "application_configurations")
	public static class ApplicationConfiguration {

		@Id
		@javax.persistence.SequenceGenerator(
				name = "application_configurations_id_seq",
				sequenceName = "application_configurations_id_seq"
		)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "application_configurations_id_seq")
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}
}
