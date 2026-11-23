package org.hibernate.orm.test.boot.jaxb.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.stream.StreamSource;

import org.hibernate.boot.UnsupportedOrmXsdVersionException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.internal.stax.MappingEventReader;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.orm.test.boot.jaxb.JaxbHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hibernate.orm.test.boot.jaxb.JaxbHelper.withStaxEventReader;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class BasicMappingJaxbTests {
	@Test
	public void unsupportedVersionTest(ServiceRegistryScope scope) {
		final var binder = new MappingBinder( scope.getRegistry() );
		final var source = new StreamSource( new StringReader( """
				<entity-mappings xmlns="http://www.hibernate.org/xsd/orm/mapping" version="99.0"/>
				""" ) );
		final var origin = new Origin( SourceType.OTHER, "unsupported-version" );

		assertThatExceptionOfType( UnsupportedOrmXsdVersionException.class )
				.isThrownBy( () -> binder.bind( source, origin ) )
				.satisfies( exception -> assertThat( exception.getRequestedVersion() ).isEqualTo( "99.0" ) );
	}

	@Test
	public void simpleUnifiedJaxbTest(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			verify( "xml/jaxb/mapping/basic/unified.xml", cls, scope );
		} );
	}

	@Test
	public void ormBaselineTest(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			verify( "xml/jaxb/mapping/basic/orm.xml", cls, scope );
		} );
	}

	private void verify(String resourceName, ClassLoaderService cls, ServiceRegistryScope scope) {
		try ( final InputStream inputStream = cls.locateResourceStream( resourceName ) ) {
			withStaxEventReader( inputStream, cls, (staxEventReader) -> {
				final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
				final XMLEventReader reader = new MappingEventReader( staxEventReader, xmlEventFactory );
				try {
					final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbEntityMappingsImpl.class );
					final JaxbEntityMappingsImpl entityMappings = JaxbHelper.VALIDATING.jaxb(
							reader,
							MappingXsdSupport.latestDescriptor().getSchema(),
							jaxbCtx
					);
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
