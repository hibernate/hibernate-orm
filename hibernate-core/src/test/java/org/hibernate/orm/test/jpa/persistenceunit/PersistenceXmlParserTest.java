/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;
import static org.hibernate.internal.util.ConfigHelper.findAsResource;

@JiraKey("HHH-18231")
public class PersistenceXmlParserTest {
	private static final String DTD_ENTITY_PU = "dtd-entity";
	private static final String DTD_ENTITY_PROPERTY = "hibernate.test.entity.property";

	@Test
	public void create_classLoaders() {
		var parser = PersistenceXmlParser.create(
				Map.of( AvailableSettings.CLASSLOADERS, List.of( new TestClassLoader( "pu1" ) ) ),
				new TestClassLoader( "pu2" ),
				null
		);
		ClassLoaderService clService = parser.getClassLoaderService();
		assertThat( clService ).isNotNull();
		assertThat( parser.parse( clService.locateResources( "META-INF/persistence.xml" ) ) )
				.containsOnlyKeys( "pu1", "pu2" );
	}

	@Test
	public void create_classLoaderService() {
		var myClassLoaderService = new ClassLoaderServiceImpl( new TestClassLoader( "pu3" ) );
		var parser = PersistenceXmlParser.create(
				// Should be ignored
				Map.of( AvailableSettings.CLASSLOADERS, List.of( new TestClassLoader( "pu1" ) ) ),
				// Should be ignored
				new TestClassLoader( "pu2" ),
				myClassLoaderService
		);
		ClassLoaderService clService = parser.getClassLoaderService();
		assertThat( clService ).isSameAs( myClassLoaderService );
		assertThat( parser.parse( clService.locateResources( "META-INF/persistence.xml" ) ) )
				.containsOnlyKeys( "pu3" );
	}

	@Test
	public void parse() {
		var parser = PersistenceXmlParser.create();
		var result = parser.parse( List.of( findPuResource( "multipu" ) ) );
		assertThat( result )
				.containsOnlyKeys( "multipu1", "multipu2", "multipu3" );
		assertThat( result.get( "multipu1" ) )
				.returns( "multipu1", PersistenceUnitDescriptor::getName )
				.returns(
						PersistenceUnitTransactionType.RESOURCE_LOCAL,
						PersistenceUnitDescriptor::getPersistenceUnitTransactionType );
		assertThat( result.get( "multipu2" ) )
				.returns( "multipu2", PersistenceUnitDescriptor::getName )
				.returns(
						PersistenceUnitTransactionType.RESOURCE_LOCAL,
						PersistenceUnitDescriptor::getPersistenceUnitTransactionType );
		assertThat( result.get( "multipu3" ) )
				.returns( "multipu3", PersistenceUnitDescriptor::getName )
				.returns( PersistenceUnitTransactionType.JTA,
						PersistenceUnitDescriptor::getPersistenceUnitTransactionType );
	}

	@Test
	public void parse_defaultTransactionType() {
		var parser = PersistenceXmlParser.create();
		var result =
				parser.parse( List.of( findPuResource( "multipu" ) ),
						PersistenceUnitTransactionType.JTA );
		assertThat( result.get( "multipu1" ) )
				.returns( "multipu1", PersistenceUnitDescriptor::getName )
				.returns(
						PersistenceUnitTransactionType.JTA,
						PersistenceUnitDescriptor::getPersistenceUnitTransactionType );
		assertThat( result.get( "multipu2" ) )
				.returns( "multipu2", PersistenceUnitDescriptor::getName )
				.returns( PersistenceUnitTransactionType.RESOURCE_LOCAL,
						PersistenceUnitDescriptor::getPersistenceUnitTransactionType );
		assertThat( result.get( "multipu3" ) )
				.returns( "multipu3", PersistenceUnitDescriptor::getName )
				.returns( PersistenceUnitTransactionType.JTA,
						PersistenceUnitDescriptor::getPersistenceUnitTransactionType );
	}

	@Test
	public void parse_rejectsDtdAndExternalEntitiesByDefault() {
		final var parser = PersistenceXmlParser.create();
		assertThatThrownBy( () -> parser.parse( List.of( findPuResource( DTD_ENTITY_PU ) ) ) )
				.isInstanceOf( MappingException.class );
	}

	@Test
	public void parse_integrationSettingAllowsDtdAndExternalEntities() {
		final var parser = PersistenceXmlParser.create( Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" ) );
		final var result = parser.parse( List.of( findPuResource( DTD_ENTITY_PU ) ) );
		assertThat( result.get( DTD_ENTITY_PU ).getProperties() )
				.containsEntry( DTD_ENTITY_PROPERTY, "resolved" );
	}

	@Test
	public void parse_integrationSettingFalseRejectsDtdAndExternalEntities() {
		final var parser = PersistenceXmlParser.create( Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, false ) );
		assertThatThrownBy( () -> parser.parse( List.of( findPuResource( DTD_ENTITY_PU ) ) ) )
				.isInstanceOf( MappingException.class );
	}

	private static URL findPuResource(String resourceName) {
		return findAsResource( "org/hibernate/jpa/test/persistenceunit/META-INF/" + resourceName + ".xml" );
	}

	private static class TestClassLoader extends ClassLoader {
		private final URL url;

		public TestClassLoader(String resourceName) {
			url = findPuResource( resourceName );
		}

		@Override
		protected Enumeration<URL> findResources(String name) {
			return name.equals( "META-INF/persistence.xml" ) ?
					Collections.enumeration( List.of( url ) ) :
					Collections.emptyEnumeration();
		}
	}
}
