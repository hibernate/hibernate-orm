/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.archive.scan.internal.DisabledScanner;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitNameTests {
	@Test
	@ServiceRegistry
	void testFirstUnit(ServiceRegistryScope scope) {
		try (EntityManagerFactory emf = loadFactory( "first", scope )) {
			assertThat( emf.getName() ).isEqualTo( "first" );
			assertThat( emf.getName() ).isEqualTo( "first" );
			assertThat( emf.getProperties() ).containsEntry( "name", "first" );
		}
	}

	@Test
	@ServiceRegistry
	void testSecondUnit(ServiceRegistryScope scope) {
		try (EntityManagerFactory emf = loadFactory( "second", scope )) {
			assertThat( emf.getName() ).isEqualTo( "second" );
			assertThat( emf.getProperties() ).containsEntry( "name", "second" );
		}
	}

	private static EntityManagerFactory loadFactory(String name, ServiceRegistryScope scope) {
		final URL puFile = PersistenceUnitNameTests.class.getClassLoader().getResource( "xml/jakarta/simple/2units.xml" );
		var descriptors = PersistenceXmlParser.create().parse( List.of( puFile ) );
		assertThat( descriptors ).containsKey( name );
		final PersistenceUnitDescriptor descriptor = descriptors.get( name );
		final EntityManagerFactoryBuilder emfBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				descriptor,
				buildSettings( scope )
		);
		return emfBuilder.build();
	}

	@SuppressWarnings("deprecation")
	private static Map<?,?> buildSettings(ServiceRegistryScope scope) {
		final ConfigurationService service = scope.getRegistry().getService( ConfigurationService.class );
		assert service != null;
		final Map<String, Object> allSettings = service.getSettings();
		final HashMap<Object, Object> settings = new HashMap<>();
		settings.put( JdbcSettings.DRIVER, allSettings.get( JdbcSettings.DRIVER ) );
		settings.put( JdbcSettings.USER, allSettings.get( JdbcSettings.USER ) );
		settings.put( JdbcSettings.PASS, allSettings.get( JdbcSettings.PASS ) );
		settings.put( JdbcSettings.URL, allSettings.get( JdbcSettings.URL ) );
		settings.put( AvailableSettings.SCANNER, DisabledScanner.class );
		return settings;
	}
}
