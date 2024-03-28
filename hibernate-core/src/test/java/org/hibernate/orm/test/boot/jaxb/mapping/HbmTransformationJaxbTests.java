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

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
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
public class HbmTransformationJaxbTests {
	@Test
	public void hbmTransformationTest(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			verifyHbm( "xml/jaxb/mapping/basic/hbm.xml", cls, scope );
		} );
	}

	private void verifyHbm(String resourceName, ClassLoaderService cls, ServiceRegistryScope scope) {
		try ( final InputStream inputStream = cls.locateResourceStream( resourceName ) ) {
			withStaxEventReader( inputStream, cls, (staxEventReader) -> {
				final XMLEventReader reader = new HbmEventReader( staxEventReader, XMLEventFactory.newInstance() );

				try {
					final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
					final JaxbHbmHibernateMapping hbmMapping = JaxbHelper.VALIDATING.jaxb( reader, MappingXsdSupport.hbmXml.getSchema(), jaxbCtx );
					assertThat( hbmMapping ).isNotNull();
					assertThat( hbmMapping.getClazz() ).hasSize( 1 );

					final JaxbEntityMappings transformed = HbmXmlTransformer.transform(
							hbmMapping,
							new Origin( SourceType.RESOURCE, resourceName ),
							() -> UnsupportedFeatureHandling.ERROR
					);

					assertThat( transformed ).isNotNull();
					assertThat( transformed.getEntities() ).hasSize( 1 );
					assertThat( transformed.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.mapping" );

					final JaxbEntity ormEntity = transformed.getEntities().get( 0 );
					assertThat( ormEntity.getName() ).isNull();
					assertThat( ormEntity.getClazz() ).isEqualTo( "SimpleEntity" );

					assertThat( ormEntity.getAttributes().getId() ).hasSize( 1 );
					assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
					assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).isEmpty();
					assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
					assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).isEmpty();
					assertThat( ormEntity.getAttributes().getDiscriminatedAssociations() ).isEmpty();
					assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).isEmpty();
					assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
					assertThat( ormEntity.getAttributes().getPluralDiscriminatedAssociations() ).isEmpty();
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
