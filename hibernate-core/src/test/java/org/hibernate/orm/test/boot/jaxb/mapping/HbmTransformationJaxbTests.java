/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.orm.test.boot.jaxb.JaxbHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.jaxb.JaxbHelper.withStaxEventReader;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class HbmTransformationJaxbTests {
	@Test
	public void hbmTransformationTest(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/basic/hbm.xml", scope, (transformed) -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );
			assertThat( transformed.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.mapping" );

			final JaxbEntityImpl ormEntity = transformed.getEntities().get( 0 );
			assertThat( ormEntity.getName() ).isNull();
			assertThat( ormEntity.getClazz() ).isEqualTo( "SimpleEntity" );

			assertThat( ormEntity.getAttributes().getIdAttributes() ).hasSize( 1 );
			assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
			assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getAnyMappingAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getPluralAnyMappingAttributes() ).isEmpty();
		} );
	}

	@Test
	@JiraKey( "HHH-20451" )
	public void mapKeyManyToManyTransformationTest(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			try ( final InputStream inputStream = cls.locateResourceStream( "xml/jaxb/mapping/ternary/hbm.xml" ) ) {
				withStaxEventReader( inputStream, cls, (staxEventReader) -> {
					final XMLEventReader reader = new HbmEventReader( staxEventReader, XMLEventFactory.newInstance() );

					try {
						final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
						final JaxbHbmHibernateMapping hbmMapping = JaxbHelper.VALIDATING.jaxb(
								reader,
								MappingXsdSupport.hbmXml.getSchema(),
								jaxbCtx
						);
						assertThat( hbmMapping ).isNotNull();
						assertThat( hbmMapping.getClazz() ).hasSize( 2 );

						final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( scope.getRegistry() )
								.addHbmXmlBinding( new Binding<>(
										hbmMapping,
										new Origin( SourceType.RESOURCE, "xml/jaxb/mapping/ternary/hbm.xml" )
								) )
								.buildMetadata();
						final List<Binding<JaxbEntityMappingsImpl>> transformedBindingList = HbmXmlTransformer.transform(
								singletonList( new Binding<>(
										hbmMapping,
										new Origin( SourceType.RESOURCE, "xml/jaxb/mapping/ternary/hbm.xml" )
								) ),
								metadata,
								UnsupportedFeatureHandling.ERROR
						);
						final JaxbEntityMappingsImpl transformed = transformedBindingList.get( 0 ).getRoot();

						assertThat( transformed ).isNotNull();
						assertThat( transformed.getEntities() ).hasSize( 2 );

						final JaxbEntityImpl mapKeyManyToManyEntity = transformed.getEntities().stream()
								.filter( e -> "MapKeyManyToManyEntity".equals( e.getClazz() ) )
								.findFirst()
								.orElseThrow();

						assertThat( mapKeyManyToManyEntity.getAttributes().getManyToManyAttributes() ).hasSize( 1 );

						final JaxbManyToManyImpl managersAttr = mapKeyManyToManyEntity.getAttributes()
								.getManyToManyAttributes()
								.get( 0 );
						assertThat( managersAttr.getName() ).isEqualTo( "managers" );
						assertThat( managersAttr.getMapKeyJoinColumns() ).hasSize( 1 );
						assertThat( managersAttr.getMapKeyJoinColumns().get( 0 ).getName() ).isEqualTo( "siteId" );
					}
					catch (JAXBException e) {
						throw new RuntimeException( "Error during JAXB processing", e );
					}
				} );
			}
			catch (IOException e) {
				throw new RuntimeException( "Error accessing mapping file", e );
			}
		} );
	}

	@Test
	public void manyToOnePropertyRefTransformationTest(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			verifyManyToOnePropertyRef( "xml/jaxb/mapping/many-to-one-property-ref/hbm.xml", cls, scope );
		} );
	}

	private void verifyManyToOnePropertyRef(String resourceName, ClassLoaderService cls, ServiceRegistryScope scope) {
		try ( final InputStream inputStream = cls.locateResourceStream( resourceName ) ) {
			withStaxEventReader( inputStream, cls, (staxEventReader) -> {
				final XMLEventReader reader = new HbmEventReader( staxEventReader, XMLEventFactory.newInstance() );

				try {
					final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
					final JaxbHbmHibernateMapping hbmMapping = JaxbHelper.VALIDATING.jaxb( reader, MappingXsdSupport.hbmXml.getSchema(), jaxbCtx );
					assertThat( hbmMapping ).isNotNull();
					assertThat( hbmMapping.getClazz() ).hasSize( 2 );

					final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( scope.getRegistry() ).addHbmXmlBinding( new Binding<>(
							hbmMapping,
							new Origin( SourceType.RESOURCE, resourceName )
					) ).buildMetadata();
					final List<Binding<JaxbEntityMappingsImpl>> transformedBindingList = HbmXmlTransformer.transform(
							singletonList( new Binding<>( hbmMapping, new Origin( SourceType.RESOURCE, resourceName ) ) ),
							metadata,
							UnsupportedFeatureHandling.ERROR
					);
					final JaxbEntityMappingsImpl transformed = transformedBindingList.get( 0 ).getRoot();

					assertThat( transformed ).isNotNull();
					assertThat( transformed.getEntities() ).hasSize( 2 );

					final JaxbEntityImpl sourceEntity = transformed.getEntities().stream()
							.filter( e -> "PropertyRefSourceEntity".equals( e.getClazz() ) )
							.findFirst()
							.orElseThrow();

					assertThat( sourceEntity.getAttributes().getManyToOneAttributes() ).hasSize( 1 );

					final JaxbManyToOneImpl manyToOne = sourceEntity.getAttributes().getManyToOneAttributes().get( 0 );
					assertThat( manyToOne.getName() ).isEqualTo( "target" );
					assertThat( manyToOne.getTargetEntity() ).isEqualTo( "PropertyRefTargetEntity" );
					assertThat( manyToOne.getPropertyRef() ).isNotNull();
					assertThat( manyToOne.getPropertyRef().getName() ).isEqualTo( "name" );
					assertThat( manyToOne.getJoinColumnOrJoinFormula() ).isEmpty();
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

	@Test
	public void testManyToManyUniqueHbmTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/manytomany/UserGroup.hbm.xml", scope, (transformed) -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl userEntity = transformed.getEntities().stream()
					.filter( e -> "User".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( userEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
			assertThat( userEntity.getAttributes().getOneToManyAttributes() ).hasSize( 1 );
			final JaxbOneToManyImpl oneToMany = userEntity.getAttributes().getOneToManyAttributes().get( 0 );

					assertThat( oneToMany.getJoinTable() ).isNotNull();
					assertThat( oneToMany.getJoinTable().getName() ).isEqualTo( "UserGroup" );
					assertThat( oneToMany.getJoinTable().getJoinColumn() ).hasSize( 1 );
					assertThat( oneToMany.getJoinTable().getJoinColumn().get( 0 ).getName() ).isEqualTo( "name" );
					assertThat( oneToMany.getJoinTable().getInverseJoinColumn() ).hasSize( 1 );
					assertThat( oneToMany.getJoinTable().getInverseJoinColumn().get( 0 ).getName() ).isEqualTo( "groupName" );

			assertThat( oneToMany.getCascade() ).isNotNull();
			assertThat( oneToMany.getCascade().getCascadeAll() ).isNotNull();

			assertThat( oneToMany.isOrphanRemoval() ).isTrue();

			assertThat( oneToMany.getMapKeyType() ).isNotNull();
			assertThat( oneToMany.getMapKeyType().getValue() ).isEqualTo( "Integer" );
		} );
	}

	@Test
	@JiraKey( "HHH-20483" )
	public void testQuotedTableName(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/manytomany/UserGroup.hbm.xml", scope, (transformed) -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl userEntity = transformed.getEntities().stream()
					.filter( e -> "User".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( userEntity.getTable() ).isNotNull();
			assertThat( userEntity.getTable().getName() ).isEqualTo( "`User`" );

			final JaxbEntityImpl groupEntity = transformed.getEntities().stream()
					.filter( e -> "Group".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( groupEntity.getTable() ).isNotNull();
			assertThat( groupEntity.getTable().getName() ).isEqualTo( "`Group`" );
		} );
	}

	private void transformAndVerify(
			String resourceName,
			ServiceRegistryScope scope,
			Consumer<JaxbEntityMappingsImpl> assertions) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			try (final InputStream inputStream = cls.locateResourceStream( resourceName )) {
				withStaxEventReader( inputStream, cls, (staxEventReader) -> {
					final XMLEventReader reader = new HbmEventReader( staxEventReader, XMLEventFactory.newInstance() );
					try {
						final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
						final JaxbHbmHibernateMapping hbmMapping = JaxbHelper.VALIDATING.jaxb(
								reader,
								MappingXsdSupport.hbmXml.getSchema(),
								jaxbCtx
						);
						assertThat( hbmMapping ).isNotNull();

						final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources(
								scope.getRegistry()
						).addHbmXmlBinding( new Binding<>(
								hbmMapping,
								new Origin( SourceType.RESOURCE, resourceName )
						) ).buildMetadata();
						final List<Binding<JaxbEntityMappingsImpl>> transformedBindingList = HbmXmlTransformer.transform(
								singletonList( new Binding<>(
										hbmMapping,
										new Origin( SourceType.RESOURCE, resourceName )
								) ),
								metadata,
								UnsupportedFeatureHandling.ERROR
						);
						final JaxbEntityMappingsImpl transformed = transformedBindingList.get( 0 ).getRoot();
						assertThat( transformed ).isNotNull();

						assertions.accept( transformed );
					}
					catch (JAXBException e) {
						throw new RuntimeException( "Error during JAXB processing", e );
					}
				} );
			}
			catch (IOException e) {
				throw new RuntimeException( "Error accessing mapping file", e );
			}
		} );
	}
}
