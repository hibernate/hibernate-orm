/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;

import org.hibernate.boot.jaxb.internal.stax.MappingEventReader;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.orm.test.boot.jaxb.JaxbHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.jaxb.JaxbHelper.withStaxEventReader;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class BasicMappingJaxbTests {
	@Test
	public void simpleUnifiedJaxbTest(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			verify( "xml/jaxb/mapping/basic/unified.xml", cls, scope );
			verify( "xml/jaxb/mapping/basic/orm.xml", cls, scope );
		} );
	}

	private void verify(String resourceName, ClassLoaderService cls, ServiceRegistryScope scope) {
		try ( final InputStream inputStream = cls.locateResourceStream( resourceName ) ) {
			withStaxEventReader( inputStream, cls, (staxEventReader) -> {
				final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
				final XMLEventReader reader = new MappingEventReader( staxEventReader, xmlEventFactory );
				try {
					final JAXBContext jaxbCtx = JAXBContext.newInstance( org.hibernate.boot.jaxb.mapping.JaxbEntityMappings.class );
					final JaxbEntityMappings entityMappings = JaxbHelper.VALIDATING.jaxb( reader, MappingXsdSupport._310.getSchema(), jaxbCtx );
					assertThat( entityMappings ).isNotNull();
					assertThat( entityMappings.getEntities() ).hasSize( 1 );
				}
				catch (JAXBException e) {
					throw new RuntimeException( "Error during JAXB processing", e );
				}
			} );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error accessing mapping file", e );
		}

	}
}
