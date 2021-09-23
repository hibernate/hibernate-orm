/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.stream.Stream;
import javax.xml.transform.stream.StreamSource;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListener;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaults;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadata;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;

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
		final MappingBinder mappingBinder = new MappingBinder( cls, true );
		final InputStream inputStream = cls.locateResourceStream( "xml/jakarta/simple/orm.xml" );
		try {
			final Binding<JaxbEntityMappings> binding = mappingBinder.bind( new StreamSource( inputStream ), new Origin( SourceType.RESOURCE, "xml/jakarta/simple/orm.xml" ) );

			assertThat( binding.getRoot().getEntity().stream().map( JaxbEntity::getClazz ) ).containsOnly( "Lighter", "ApplicationServer" );

			final JaxbPersistenceUnitMetadata puMetadata = binding.getRoot().getPersistenceUnitMetadata();
			final JaxbPersistenceUnitDefaults puDefaults = puMetadata.getPersistenceUnitDefaults();
			final Stream<String> listenerNames = puDefaults.getEntityListeners()
					.getEntityListener()
					.stream()
					.map( JaxbEntityListener::getClazz );
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
		final URL url = cls.locateResource( "xml/jakarta/simple/persistence.xml" );
		final ParsedPersistenceXmlDescriptor descriptor = PersistenceXmlParser.locateIndividualPersistenceUnit( url, Collections.emptyMap() );
		assertThat( descriptor.getName() ).isEqualTo( "defaultpar" );
		assertThat( descriptor.getManagedClassNames() ).contains( "org.hibernate.jpa.test.pack.defaultpar.Lighter" );
	}
}
