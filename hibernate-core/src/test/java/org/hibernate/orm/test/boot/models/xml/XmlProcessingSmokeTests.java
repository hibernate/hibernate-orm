/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.spi.FilterDefRegistration;
import org.hibernate.boot.models.xml.internal.XmlDocumentContextImpl;
import org.hibernate.boot.models.xml.internal.XmlDocumentImpl;
import org.hibernate.boot.models.xml.internal.XmlPreProcessingResultImpl;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.internal.StringTypeDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
import org.hibernate.orm.test.boot.models.XmlHelper;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.AccessType.FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.annotations.CascadeType.LOCK;
import static org.hibernate.annotations.CascadeType.PERSIST;
import static org.hibernate.annotations.CascadeType.REMOVE;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class XmlProcessingSmokeTests {
	@Test
	void testGlobals() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();
		collector.addDocument( XmlHelper.loadMapping( "mappings/models/globals.xml", SIMPLE_CLASS_LOADING ) );
	}

	@Test
	void testPersistenceUnitDefaults1() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl simple1 = XmlHelper.loadMapping( "mappings/models/simple1.xml", SIMPLE_CLASS_LOADING );
		final JaxbEntityMappingsImpl simple2 = XmlHelper.loadMapping( "mappings/models/simple2.xml", SIMPLE_CLASS_LOADING );
		collector.addDocument( simple1 );
		collector.addDocument( simple2);

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE, LOCK ) );

		// simple2.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog2" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema2" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testPersistenceUnitDefaults2() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		collector.addDocument( XmlHelper.loadMapping( "mappings/models/simple2.xml", SIMPLE_CLASS_LOADING ) );
		collector.addDocument( XmlHelper.loadMapping( "mappings/models/simple1.xml", SIMPLE_CLASS_LOADING ) );

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE, LOCK ) );

		// simple1.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog1" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema1" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testSimpleXmlDocumentBuilding() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl simple1 = XmlHelper.loadMapping( "mappings/models/simple1.xml", SIMPLE_CLASS_LOADING );
		final JaxbEntityMappingsImpl simple2 = XmlHelper.loadMapping( "mappings/models/simple2.xml", SIMPLE_CLASS_LOADING );
		collector.addDocument( simple1 );
		collector.addDocument( simple2 );

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();

		final XmlDocumentImpl simple1Doc = XmlDocumentImpl.consume( simple1, metadata );
		assertThat( simple1Doc.getDefaults().getPackage() ).isEqualTo( "org.hibernate.models.orm.xml" );
		assertThat( simple1Doc.getEntityMappings() ).hasSize( 1 );
		assertThat( simple1Doc.getEntityMappings().get( 0 ).getClazz() ).isEqualTo( "SimpleEntity" );
		assertThat( simple1Doc.getEntityMappings().get( 0 ).getName() ).isNull();
		assertThat( simple1Doc.getEntityMappings().get( 0 ).isMetadataComplete() ).isNull();

		final XmlDocumentImpl simple2Doc = XmlDocumentImpl.consume( simple2, metadata );
		assertThat( simple2Doc.getDefaults().getPackage() ).isNull();
		assertThat( simple2Doc.getEntityMappings() ).hasSize( 1 );
		assertThat( simple2Doc.getEntityMappings().get( 0 ).getClazz() ).isNull();
		assertThat( simple2Doc.getEntityMappings().get( 0 ).getName() ).isEqualTo( "DynamicEntity" );
		assertThat( simple2Doc.getEntityMappings().get( 0 ).isMetadataComplete() ).isTrue();
	}

	@Test
	@ServiceRegistry
	void testSimpleGlobalXmlProcessing(ServiceRegistryScope scope) {
		final SourceModelBuildingContext buildingContext = SourceModelTestHelper.createBuildingContext( StringTypeDescriptor.class );
		final XmlPreProcessingResultImpl collectedXmlResources = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl xmlMapping = XmlHelper.loadMapping( "mappings/models/globals.xml", SIMPLE_CLASS_LOADING );
		collectedXmlResources.addDocument( xmlMapping );

		final DomainModelCategorizationCollector collector = new DomainModelCategorizationCollector(
				false,
				new GlobalRegistrationsImpl( buildingContext, new BootstrapContextImpl() ),
				buildingContext
		);
		collectedXmlResources.getDocuments().forEach( jaxbEntityMappings -> {
			final XmlDocumentContextImpl xmlDocumentContext = new XmlDocumentContextImpl(
					XmlDocumentImpl.consume( jaxbEntityMappings, collectedXmlResources.getPersistenceUnitMetadata() ),
					new RootMappingDefaults(
							new MetadataBuilderImpl.MappingDefaultsImpl( scope.getRegistry() ),
							collectedXmlResources.getPersistenceUnitMetadata()
					),
					buildingContext,
					new BootstrapContextImpl()
			);
			collector.apply( jaxbEntityMappings, xmlDocumentContext );
		} );

		final GlobalRegistrationsImpl globalRegistrations = collector.getGlobalRegistrations();
		assertThat( globalRegistrations.getJavaTypeRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getJavaTypeRegistrations().get(0).getDescriptor().getClassName() )
				.isEqualTo( StringTypeDescriptor.class.getName() );

		assertThat( globalRegistrations.getJdbcTypeRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getJdbcTypeRegistrations().get(0).getDescriptor().getClassName() )
				.isEqualTo( ClobJdbcType.class.getName() );

		assertThat( globalRegistrations.getUserTypeRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getUserTypeRegistrations().get(0).getUserTypeClass().getClassName() )
				.isEqualTo( MyUserType.class.getName() );

		assertThat( globalRegistrations.getConverterRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getConverterRegistrations().get(0).getConverterType() )
				.isEqualTo( org.hibernate.type.YesNoConverter.class );

		validateFilterDefs( globalRegistrations.getFilterDefRegistrations() );
	}

	private void validateFilterDefs(Map<String, FilterDefRegistration> filterDefRegistrations) {
		assertThat( filterDefRegistrations ).hasSize( 2 );

		final FilterDefRegistration amountFilter = filterDefRegistrations.get( "amount_filter" );
		assertThat( amountFilter.getDefaultCondition() ).isEqualTo( "amount = :amount" );
		assertThat( amountFilter.getParameterTypes() ).hasSize( 1 );
		final ClassDetails amountParameterType = amountFilter.getParameterTypes().get( "amount" );
		assertThat( amountParameterType.getClassName() ).isEqualTo( Integer.class.getName() );

		final FilterDefRegistration nameFilter = filterDefRegistrations.get( "name_filter" );
		assertThat( nameFilter.getDefaultCondition() ).isEqualTo( "name = :name" );
		assertThat( nameFilter.getParameterTypes() ).hasSize( 1 );
		final ClassDetails nameParameterType = nameFilter.getParameterTypes().get( "name" );
		assertThat( nameParameterType.getClassName() ).isEqualTo( String.class.getName() );
	}
}
