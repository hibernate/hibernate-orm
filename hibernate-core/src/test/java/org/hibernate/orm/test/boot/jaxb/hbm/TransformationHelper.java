/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.jaxb.hbm;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.orm.test.boot.jaxb.JaxbHelper;
import org.hibernate.service.ServiceRegistry;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.jaxb.JaxbHelper.withStaxEventReader;

/**
 * @author Steve Ebersole
 */
public class TransformationHelper {
	public static JaxbEntityMappingsImpl transform(String resourceName, ServiceRegistry serviceRegistry) {
		final ClassLoaderService cls = serviceRegistry.requireService( ClassLoaderService.class );
		try ( final InputStream inputStream = cls.locateResourceStream( resourceName ) ) {
			return withStaxEventReader( inputStream, cls, (staxEventReader) -> {
				final XMLEventReader reader = new HbmEventReader( staxEventReader, XMLEventFactory.newInstance() );

				try {
					final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
					final JaxbHbmHibernateMapping hbmMapping = JaxbHelper.VALIDATING.jaxb(
							reader,
							MappingXsdSupport.hbmXml.getSchema(),
							jaxbCtx
					);
					assertThat( hbmMapping ).isNotNull();
					assertThat( hbmMapping.getClazz() ).hasSize( 1 );

					final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry ).addHbmXmlBinding( new Binding<>(
							hbmMapping,
							new Origin( SourceType.RESOURCE, resourceName )
					) ).buildMetadata();
					final List<Binding<JaxbEntityMappingsImpl>> transformed = HbmXmlTransformer.transform(
							Collections.singletonList( new Binding<>( hbmMapping, new Origin( SourceType.RESOURCE, resourceName ) ) ),
							metadata,
							serviceRegistry,
							UnsupportedFeatureHandling.ERROR
					);

					return transformed.get(0).getRoot();
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

	/**
	 * Verify correctness of the transformed mapping by marshalling and unmarshalling it
	 * using the JaxbEntityMappings JAXBContext
	 */
	static void verifyTransformation(JaxbEntityMappingsImpl transformed) {
		try {
			final JAXBContext jaxbContext = JAXBContext.newInstance( JaxbEntityMappingsImpl.class );
			final Marshaller marshaller = jaxbContext.createMarshaller();
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			final StringWriter stringWriter = new StringWriter();
			marshaller.marshal( transformed, stringWriter );

			final String transformedXml = stringWriter.toString();

			final StringReader stringReader = new StringReader( transformedXml );
			final JaxbEntityMappingsImpl unmarshalled = (JaxbEntityMappingsImpl) unmarshaller.unmarshal( stringReader );

			assertThat( unmarshalled ).isNotNull();
		}
		catch (JAXBException e) {
			throw new RuntimeException( "Unable to create JAXBContext for JaxbEntityMappings", e );
		}
	}
}
