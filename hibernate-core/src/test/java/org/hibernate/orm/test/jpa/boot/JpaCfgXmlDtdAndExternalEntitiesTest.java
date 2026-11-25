/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.PersistenceConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CFG_XML_FILE;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;

/**
 * Tests that {@code hibernate.xml.allow_dtd_and_external_entities} is honored for the
 * {@code cfg.xml} selected via {@code hibernate.cfg_xml_file} on the JPA bootstrap path.
 */
@BaseUnitTest
@JiraKey("HHH-21193")
class JpaCfgXmlDtdAndExternalEntitiesTest {
	private static final String DTD_ENTITY_CFG_XML = "org/hibernate/orm/test/boot/cfgXml/dtd-entity.cfg.xml";
	private static final String DTD_ENTITY_PROPERTY = "hibernate.test.entity.property";

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void selectedHibernateXmlRejectsDtdAndExternalEntitiesByDefault(boolean hibernateConfiguration) {
		final var configuration = configuration( hibernateConfiguration ).property( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		assertThatThrownBy( () -> builder( configuration ) ).isInstanceOf( HibernateException.class );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void unitPropertyAllowsDtdAndExternalEntitiesInSelectedHibernateXml(boolean hibernateConfiguration) {
		final var configuration = configuration( hibernateConfiguration )
				.property( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" )
				.property( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		final var builder = builder( configuration );
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( DTD_ENTITY_PROPERTY, "resolved" );
		}
		finally {
			builder.cancel();
		}
	}

	@Test
	void integrationSettingAllowsDtdAndExternalEntitiesInSelectedHibernateXml() {
		final var builder = new EntityManagerFactoryBuilderImpl(
				new PersistenceConfigurationDescriptor( configuration( false ) ),
				Map.of( CFG_XML_FILE, DTD_ENTITY_CFG_XML, XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, true ) );
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( DTD_ENTITY_PROPERTY, "resolved" );
		}
		finally {
			builder.cancel();
		}
	}

	@Test
	void integrationSettingOverridesUnitPropertyForSelectedHibernateXml() {
		final var configuration = configuration( false )
				.property( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" )
				.property( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		assertThatThrownBy( () -> new EntityManagerFactoryBuilderImpl(
				new PersistenceConfigurationDescriptor( configuration ),
				Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, false ) ) )
				.isInstanceOf( HibernateException.class );
	}

	private static PersistenceConfiguration configuration(boolean hibernateConfiguration) {
		final PersistenceConfiguration configuration = hibernateConfiguration
				? new HibernatePersistenceConfiguration( "assembly" ) : new PersistenceConfiguration( "assembly" );
		return configuration.property( DIALECT, H2Dialect.class.getName() )
				.property( ALLOW_METADATA_ON_BOOT, false )
				.property( CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() )
				.property( URL, "jdbc:unit" ).property( DRIVER, "unit-driver" );
	}

	private static EntityManagerFactoryBuilderImpl builder(PersistenceConfiguration configuration) {
		return new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration ), Map.of() );
	}
}
