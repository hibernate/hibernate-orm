/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.jakarta;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class JakartaXmlSmokeTests {
	@Test
	public void testLoadingOrmXml(ServiceRegistryScope scope) {
		final ClassLoaderService cls = scope.getRegistry().getService( ClassLoaderService.class );
		final MappingBinder mappingBinder = new MappingBinder( cls, MappingBinder.DEFAULT_VALIDATING );
		final String resourceName = "xml/jakarta/simple/orm.xml";
		final InputStream inputStream = cls.locateResourceStream( resourceName );
		try {
			final Binding<JaxbEntityMappingsImpl> binding = mappingBinder.bind( inputStream, new Origin( SourceType.RESOURCE, resourceName ) );
			assertThat( binding.getRoot()
					.getEntities()
					.stream()
					.map( JaxbEntityImpl::getClazz ) ).containsOnly( "Lighter", "ApplicationServer" );

			final JaxbPersistenceUnitMetadataImpl puMetadata = binding.getRoot().getPersistenceUnitMetadata();
			final JaxbPersistenceUnitDefaultsImpl puDefaults = puMetadata.getPersistenceUnitDefaults();
			final Stream<String> listenerNames = puDefaults.getEntityListenerContainer()
					.getEntityListeners()
					.stream()
					.map( JaxbEntityListenerImpl::getClazz );
			assertThat( listenerNames ).containsOnly( "org.hibernate.jpa.test.pack.defaultpar.IncrementListener" );
		}
		finally {
			try {
				inputStream.close();
			}
			catch (IOException ignore) {
			}
		}
	}

	@Test
	public void testLoadingPersistenceXml(ServiceRegistryScope scope) {
		final ClassLoaderService cls = scope.getRegistry().getService( ClassLoaderService.class );
		final Map<String, PersistenceUnitDescriptor> descriptors = PersistenceXmlParser.create()
				.parse( cls.locateResources( "xml/jakarta/simple/persistence.xml" ) );
		String expectedPuName = "defaultpar";
		assertThat( descriptors ).containsOnlyKeys( expectedPuName );
		var descriptor = descriptors.get( expectedPuName );
		assertThat( descriptor.getName() ).isEqualTo( expectedPuName );
		assertThat( descriptor.getManagedClassNames() ).contains( "org.hibernate.jpa.test.pack.defaultpar.Lighter" );
	}
}
