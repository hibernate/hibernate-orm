/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.jakarta;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.*;


/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop" )
		}
)
@DomainModel( annotatedClasses = SimpleEntity.class )
@SessionFactory
public class JakartaSchemaToolingTests {
	@Test
	public void testSchemaCreation() {
		verifySchemaCreation( JAKARTA_HBM2DDL_DATABASE_ACTION, JAKARTA_JDBC_DRIVER, JAKARTA_JDBC_URL, JAKARTA_JDBC_USER, JAKARTA_JDBC_PASSWORD );
		verifySchemaCreation( HBM2DDL_DATABASE_ACTION, JPA_JDBC_DRIVER, JPA_JDBC_URL, JPA_JDBC_USER, JPA_JDBC_PASSWORD );
	}

	private void verifySchemaCreation(
			String actionSettingName,
			String driverSettingName,
			String urlSettingName,
			String userSettingName,
			String passwordSettingName) {
		final SessionFactoryImplementor sessionFactory = buildSessionFactory(
				actionSettingName, Action.CREATE_DROP,
				driverSettingName, Environment.getProperties().get( AvailableSettings.DRIVER ),
				urlSettingName, resolveUrl( (String) Environment.getProperties().get( AvailableSettings.URL ) ),
				userSettingName, resolveUsername( (String) Environment.getProperties().get( AvailableSettings.USER ) ),
				passwordSettingName, Environment.getProperties().get( AvailableSettings.PASS )
		);
		try {
			tryQuery( sessionFactory );
		}
		finally {
			sessionFactory.close();
		}

	}

	private void tryQuery(SessionFactoryImplementor sessionFactory) {
		TransactionUtil2.inTransaction( sessionFactory, (session) -> {
			// the query would fail if the schema were not exported - just a smoke test
			session.createQuery( "from SimpleEntity" ).list();
		});
	}

	@Test
	public void testPrecedence() {
		// make sure JAKARTA_HBM2DDL_DATABASE_ACTION (`jakarta...`) takes precedence over HBM2DDL_DATABASE_ACTION (`javax...`)
		try ( SessionFactoryImplementor sessionFactory = buildSessionFactory(
				JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP,
				HBM2DDL_DATABASE_ACTION, Action.NONE,
				JAKARTA_JDBC_DRIVER, Environment.getProperties().get( AvailableSettings.DRIVER ),
				JPA_JDBC_DRIVER, "does.not.exist",
				JAKARTA_JDBC_URL, resolveUrl( (String) Environment.getProperties().get( AvailableSettings.URL ) ),
				JPA_JDBC_URL, "jdbc:na:nowhere",
				JAKARTA_JDBC_USER, resolveUsername( (String) Environment.getProperties().get( AvailableSettings.USER ) ),
				JPA_JDBC_USER, "goofy",
				JAKARTA_JDBC_PASSWORD, Environment.getProperties().get( AvailableSettings.PASS ),
				JPA_JDBC_PASSWORD, "goober"
		) ) {
			tryQuery( sessionFactory );
		}
	}

	@Test
	public void testCreateDropWithFailureInBetween() {
		// Make sure that when using the "create-drop" database action, when a failure occur after schema is created,
		// the schema is correctly dropped.
		assertThatThrownBy( () -> buildSessionFactory(
				JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP,
				JAKARTA_JDBC_DRIVER, Environment.getProperties().get( AvailableSettings.DRIVER ),
				JAKARTA_JDBC_URL, resolveUrl( (String) Environment.getProperties().get( AvailableSettings.URL ) ),
				JAKARTA_JDBC_USER, resolveUsername( (String) Environment.getProperties().get( AvailableSettings.USER ) ),
				JAKARTA_JDBC_PASSWORD, Environment.getProperties().get( AvailableSettings.PASS ),
				// Simulates a failure from e.g. the Hibernate Search observer
				AvailableSettings.SESSION_FACTORY_OBSERVER, new SessionFactoryObserver() {
					@Override
					public void sessionFactoryCreated(org.hibernate.SessionFactory factory) {
						throw new RuntimeException( "Simulated failure" );
					}
				}
		) )
				.hasRootCauseMessage( "Simulated failure" );

		// Now check that the schema was dropped: queries should fail.
		try ( SessionFactoryImplementor sessionFactory = buildSessionFactory(
				JAKARTA_HBM2DDL_DATABASE_ACTION, Action.NONE,
				JAKARTA_JDBC_DRIVER, Environment.getProperties().get( AvailableSettings.DRIVER ),
				JAKARTA_JDBC_URL, resolveUrl( (String) Environment.getProperties().get( AvailableSettings.URL ) ),
				JAKARTA_JDBC_USER, resolveUsername( (String)Environment.getProperties().get( AvailableSettings.USER ) ),
				JAKARTA_JDBC_PASSWORD, Environment.getProperties().get( AvailableSettings.PASS )
		) ) {
			assertThatThrownBy( () -> tryQuery( sessionFactory ) ).isNotNull();
		}
	}

	public static void applyToProperties(Properties properties, Object... pairs) {
		assert pairs.length % 2 == 0;
		for ( int i = 0; i < pairs.length; i+=2 ) {
			properties.put( pairs[i], pairs[i+1] );
		}
	}

	private SessionFactoryImplementor buildSessionFactory(Object... settingPairs) {
		final Properties settings = new Properties();
		settings.setProperty( AvailableSettings.AUTOCOMMIT, "false" );
		settings.setProperty( AvailableSettings.POOL_SIZE, "5" );
		settings.setProperty( DriverManagerConnectionProvider.INITIAL_SIZE, "0" );
		settings.setProperty(
				DriverManagerConnectionProvider.INIT_SQL,
				Environment.getProperties().getProperty( DriverManagerConnectionProvider.INIT_SQL )
		);
		applyToProperties( settings, settingPairs );
		ServiceRegistryUtil.applySettings( settings );

		final PersistenceUnitDescriptorAdapter puDescriptor = new PersistenceUnitDescriptorAdapter() {
			@Override
			public Properties getProperties() {
				return settings;
			}

			@Override
			public List<String> getManagedClassNames() {
				return Collections.singletonList( SimpleEntity.class.getName() );
			}
		};


		final EntityManagerFactoryBuilder emfBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				puDescriptor,
				PropertiesHelper.map( settings ),
				(mergedSettings) -> mergedSettings.getConfigurationValues().clear()
		);

		return emfBuilder.build().unwrap( SessionFactoryImplementor.class );
	}
}
