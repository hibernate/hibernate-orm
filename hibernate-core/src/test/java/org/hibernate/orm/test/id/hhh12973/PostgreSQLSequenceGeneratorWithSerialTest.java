/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.hhh12973;

import java.io.StringReader;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.id.SequenceMismatchStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Rule;
import org.junit.jupiter.api.Test;


import static org.hibernate.id.enhanced.SequenceGeneratorLogger.SEQUENCE_GENERATOR_MESSAGE_LOGGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12973")
@RequiresDialect(value = PostgreSQLDialect.class)
public class PostgreSQLSequenceGeneratorWithSerialTest extends EntityManagerFactoryBasedFunctionalTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( SEQUENCE_GENERATOR_MESSAGE_LOGGER );

	private final Triggerable triggerable = logInspection.watchForLogMessages( "HHH090202:" );

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
	protected void addConfigOptions(Map settings) {
		triggerable.reset();
		assertFalse( triggerable.wasTriggered() );

		//For this test, we need to make sure the DB is created prior to bootstrapping Hibernate
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		SessionFactory sessionFactory = null;

		try {
			Configuration config = new Configuration();
			sessionFactory = config.buildSessionFactory( ssr );

			try (Session session = sessionFactory.openSession()) {
				session.doWork( connection -> {
					try (Statement statement = connection.createStatement()) {
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

		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE, new StringReader(
				DROP_TABLE + ";" + DROP_SEQUENCE
		) );
		settings.put( AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, SequenceMismatchStrategy.FIX );
	}

	@Override
	protected boolean exportSchema() {
		return false;
	}

	@Override
	protected void entityManagerFactoryBuilt(EntityManagerFactory factory) {
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	public void test() {

		final AtomicLong id = new AtomicLong();

		final int ITERATIONS = 51;

		inTransaction( entityManager -> {
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
		@jakarta.persistence.SequenceGenerator(
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
