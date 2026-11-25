/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.boot;

import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

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

	@Test
	void selectedHibernateXmlRejectsDtdAndExternalEntitiesByDefault() {
		final Properties properties = baseProperties();
		properties.put( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		assertThatThrownBy( () -> builder( descriptor( properties ), Map.of() ) )
				.isInstanceOf( HibernateException.class );
	}

	@Test
	void unitPropertyAllowsDtdAndExternalEntitiesInSelectedHibernateXml() {
		final Properties properties = baseProperties();
		properties.put( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" );
		properties.put( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		final EntityManagerFactoryBuilderImpl builder = builder( descriptor( properties ), Map.of() );
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( DTD_ENTITY_PROPERTY, "resolved" );
		}
		finally {
			builder.cancel();
		}
	}

	@Test
	void integrationSettingAllowsDtdAndExternalEntitiesInSelectedHibernateXml() {
		final EntityManagerFactoryBuilderImpl builder = builder(
				descriptor( baseProperties() ),
				Map.of( CFG_XML_FILE, DTD_ENTITY_CFG_XML, XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, true )
		);
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( DTD_ENTITY_PROPERTY, "resolved" );
		}
		finally {
			builder.cancel();
		}
	}

	@Test
	void integrationSettingOverridesUnitPropertyForSelectedHibernateXml() {
		final Properties properties = baseProperties();
		properties.put( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" );
		properties.put( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		assertThatThrownBy( () -> builder(
				descriptor( properties ),
				Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, false )
		) ).isInstanceOf( HibernateException.class );
	}

	private static Properties baseProperties() {
		final Properties properties = new Properties();
		properties.put( DIALECT, H2Dialect.class.getName() );
		properties.put( ALLOW_METADATA_ON_BOOT, false );
		properties.put( CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() );
		properties.put( URL, "jdbc:unit" );
		properties.put( DRIVER, "unit-driver" );
		return properties;
	}

	private static PersistenceUnitDescriptor descriptor(Properties properties) {
		return new PersistenceUnitDescriptorAdapter() {
			@Override
			public Properties getProperties() {
				return properties;
			}
		};
	}

	private static EntityManagerFactoryBuilderImpl builder(
			PersistenceUnitDescriptor descriptor,
			Map<String, Object> integrationSettings) {
		return new EntityManagerFactoryBuilderImpl( descriptor, integrationSettings );
	}
}
