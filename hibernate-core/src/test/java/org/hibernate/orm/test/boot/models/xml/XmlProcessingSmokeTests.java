/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml;

import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer;
import org.hibernate.boot.mapping.internal.categorize.FetchProfileRegistration;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.pipeline.internal.source.AvailableResources;
import org.hibernate.boot.pipeline.internal.source.AvailableResourcesContext;
import org.hibernate.boot.models.spi.FilterDefRegistration;
import org.hibernate.boot.models.spi.NamedNativeQueryRegistration;
import org.hibernate.boot.models.spi.NamedQueryRegistration;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadata;
import org.hibernate.boot.mapping.internal.xml.XmlDocumentContextImpl;
import org.hibernate.boot.mapping.internal.xml.XmlDocumentImpl;
import org.hibernate.boot.mapping.internal.xml.XmlPreProcessingResultImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.models.internal.StringTypeDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
import org.hibernate.orm.test.boot.models.XmlHelper;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.SqlResultSetMapping;

import static jakarta.persistence.AccessType.FIELD;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class XmlProcessingSmokeTests {
	@Test
	void testGlobals() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();
		collector.addDocument( XmlHelper.bindMapping( "mappings/models/globals.xml", SIMPLE_CLASS_LOADING ) );
	}

	@Test
	void testPersistenceUnitDefaults1() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final Binding<JaxbEntityMappingsImpl> simple1 = XmlHelper.bindMapping( "mappings/models/simple1.xml", SIMPLE_CLASS_LOADING );
		final Binding<JaxbEntityMappingsImpl> simple2 = XmlHelper.bindMapping( "mappings/models/simple2.xml", SIMPLE_CLASS_LOADING );
		collector.addDocument( simple1 );
		collector.addDocument( simple2);

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE ) );

		// simple2.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog2" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema2" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testPersistenceUnitDefaults2() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		collector.addDocument( XmlHelper.bindMapping( "mappings/models/simple2.xml", SIMPLE_CLASS_LOADING ) );
		collector.addDocument( XmlHelper.bindMapping( "mappings/models/simple1.xml", SIMPLE_CLASS_LOADING ) );

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE ) );

		// simple1.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog1" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema1" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testSimpleXmlDocumentBuilding() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final Binding<JaxbEntityMappingsImpl> simple1 = XmlHelper.bindMapping( "mappings/models/simple1.xml", SIMPLE_CLASS_LOADING );
		final Binding<JaxbEntityMappingsImpl> simple2 = XmlHelper.bindMapping( "mappings/models/simple2.xml", SIMPLE_CLASS_LOADING );
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
		final ModelsContext buildingContext = SourceModelTestHelper.createBuildingContext( StringTypeDescriptor.class );
		final XmlPreProcessingResultImpl collectedXmlResources = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl xmlMapping = XmlHelper.loadMapping( "mappings/models/globals.xml", SIMPLE_CLASS_LOADING );
		final Binding<JaxbEntityMappingsImpl> binding = new Binding<>( xmlMapping, new Origin( SourceType.RESOURCE, "mappings/models/globals.xml" ) );
		collectedXmlResources.addDocument( binding );

		final DomainModelCategorizationCollector collector = new DomainModelCategorizationCollector(
				new GlobalRegistrationsImpl( buildingContext, new BootstrapContextImpl() ),
				buildingContext
		);
		collectedXmlResources.getDocuments().forEach( xmlDocument -> {
			final XmlDocumentContextImpl xmlDocumentContext = new XmlDocumentContextImpl(
					xmlDocument,
					new RootMappingDefaults(
							new MetadataBuilderImpl.MappingDefaultsImpl( scope.getRegistry() ),
							collectedXmlResources.getPersistenceUnitMetadata()
					),
					buildingContext,
					new BootstrapContextImpl()
			);
			collector.apply( xmlMapping, xmlDocumentContext );
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

	@Test
	@ServiceRegistry
	void testGlobalNamedQueryHints(ServiceRegistryScope scope) {
		final ModelsContext buildingContext = SourceModelTestHelper.createBuildingContext( StringTypeDescriptor.class );
		final XmlPreProcessingResultImpl collectedXmlResources = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl xmlMapping = XmlHelper.loadMapping(
				"mappings/models/named-query-hints.xml",
				SIMPLE_CLASS_LOADING
		);
		final Binding<JaxbEntityMappingsImpl> binding = new Binding<>(
				xmlMapping,
				new Origin( SourceType.RESOURCE, "mappings/models/named-query-hints.xml" )
		);
		collectedXmlResources.addDocument( binding );

		final DomainModelCategorizationCollector collector = new DomainModelCategorizationCollector(
				new GlobalRegistrationsImpl( buildingContext, new BootstrapContextImpl() ),
				buildingContext
		);
		collectedXmlResources.getDocuments().forEach( xmlDocument -> {
			final XmlDocumentContextImpl xmlDocumentContext = new XmlDocumentContextImpl(
					xmlDocument,
					new RootMappingDefaults(
							new MetadataBuilderImpl.MappingDefaultsImpl( scope.getRegistry() ),
							collectedXmlResources.getPersistenceUnitMetadata()
					),
					buildingContext,
					new BootstrapContextImpl()
			);
			collector.apply( xmlMapping, xmlDocumentContext );
		} );

		final GlobalRegistrationsImpl globalRegistrations = collector.getGlobalRegistrations();

		final NamedQueryRegistration namedQuery = globalRegistrations.getNamedQueryRegistrations().get( "global.findAll" );
		assertThat( namedQuery ).isNotNull();
		assertThat( namedQuery.getQueryHints() ).containsEntry( "org.hibernate.timeout", "200" );

		final NamedNativeQueryRegistration nativeQuery =
				globalRegistrations.getNamedNativeQueryRegistrations().get( "global.findAllNative" );
		assertThat( nativeQuery ).isNotNull();
		assertThat( nativeQuery.getQueryHints() ).containsEntry( "org.hibernate.timeout", "300" );

		assertThat( globalRegistrations.getNamedStoredProcedureQueryRegistrations().get( "global.proc" ) )
				.isNotNull()
				.extracting( registration -> registration.configuration().hints() )
				.satisfies( hints -> assertThat( hints )
						.hasSize( 1 )
						.anySatisfy( hint -> {
							assertThat( hint.name() ).isEqualTo( "org.hibernate.timeout" );
							assertThat( hint.value() ).isEqualTo( "400" );
						} ) );
	}

	@Test
	@ServiceRegistry
	void testRootGlobalXmlProcessing(ServiceRegistryScope scope) {
		final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( scope.getRegistry() );
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.mappingFile( "mappings/models/xml-global-objects.xml" );
		final AvailableResources availableResources = AvailableResources.from(
				persistenceConfiguration,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				)
		);
		final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
				availableResources,
				metadataBuildingContext
		);

		final org.hibernate.boot.mapping.internal.categorize.GlobalRegistrationsImpl globalRegistrations =
				(org.hibernate.boot.mapping.internal.categorize.GlobalRegistrationsImpl) categorizedDomainModel.getGlobalRegistrations();

		assertThat( globalRegistrations.getConverterRegistrations() ).singleElement().satisfies( (registration) -> {
			assertThat( registration.converterType().getClassName() ).isEqualTo( "org.hibernate.type.YesNoConverter" );
			assertThat( registration.explicitDomainType().toJavaClass() ).isEqualTo( boolean.class );
			assertThat( registration.autoApply() ).isTrue();
		} );
		assertThat( globalRegistrations.getJpaConverters() ).singleElement().satisfies( (registration) -> {
			assertThat( registration.converterType().getClassName() )
					.isEqualTo( "org.hibernate.type.NumericBooleanConverter" );
			assertThat( registration.autoApply() ).isFalse();
		} );
		assertThat( globalRegistrations.getImportedRenames() )
				.containsEntry( "XmlSimpleEntity", "org.hibernate.orm.test.boot.models.xml.SimpleEntity" );

		final FetchProfileRegistration fetchProfile = globalRegistrations.getFetchProfileRegistrations().get( 0 );
		assertThat( fetchProfile.getName() ).isEqualTo( "customer-with-orders" );
		assertThat( fetchProfile.getFetchOverrides() ).singleElement().satisfies( (fetchOverride) -> {
			assertThat( fetchOverride.entityName() ).isEqualTo( "Customer" );
			assertThat( fetchOverride.association() ).isEqualTo( "orders" );
			assertThat( fetchOverride.style() ).isEqualTo( "join" );
		} );

		final NamedQuery namedQuery = (NamedQuery) globalRegistrations.getNamedQueryRegistrations()
				.get( "rootHqlQuery" )
				.getConfiguration();
		assertThat( namedQuery.query() ).isEqualTo( "from Customer" );
		assertThat( namedQuery.hints() ).singleElement().satisfies( (hint) -> {
			assertThat( hint.name() ).isEqualTo( "root.hint" );
			assertThat( hint.value() ).isEqualTo( "root-value" );
		} );

		final NamedNativeQuery nativeQuery = (NamedNativeQuery) globalRegistrations.getNamedNativeQueryRegistrations()
				.get( "rootNativeQuery" )
				.getConfiguration();
		assertThat( nativeQuery.query() ).isEqualTo( "select id, name from customers" );
		assertThat( nativeQuery.columns() ).singleElement().satisfies( (columnResult) -> {
			assertThat( columnResult.name() ).isEqualTo( "id" );
			assertThat( columnResult.type() ).isEqualTo( Long.class );
		} );

		final NamedStoredProcedureQuery storedProcedureQuery = (NamedStoredProcedureQuery) globalRegistrations
				.getNamedStoredProcedureQueryRegistrations()
				.get( "rootStoredProcedure" )
				.getConfiguration();
		assertThat( storedProcedureQuery.procedureName() ).isEqualTo( "sp_customers" );
		assertThat( storedProcedureQuery.parameters() ).singleElement().satisfies( (parameter) -> {
			assertThat( parameter.name() ).isEqualTo( "name" );
			assertThat( parameter.type() ).isEqualTo( String.class );
		} );

		final SqlResultSetMapping sqlResultSetMapping = globalRegistrations.getSqlResultSetMappingRegistrations()
				.get( "rootResultSetMapping" )
				.configuration();
		assertThat( sqlResultSetMapping.columns() ).singleElement().satisfies( (columnResult) -> {
			assertThat( columnResult.name() ).isEqualTo( "name" );
			assertThat( columnResult.type() ).isEqualTo( String.class );
		} );

		final var databaseObject = globalRegistrations.getDatabaseObjectRegistrations().get( 0 );
		assertThat( databaseObject.create() ).isEqualTo( "create sequence xml_global_sequence" );
		assertThat( databaseObject.drop() ).isEqualTo( "drop sequence xml_global_sequence" );
		final var dialectScope = databaseObject.dialectScopes().get( 0 );
		assertThat( dialectScope.name() ).isEqualTo( "org.hibernate.dialect.H2Dialect" );
	}

	private void validateFilterDefs(Map<String, FilterDefRegistration> filterDefRegistrations) {
		assertThat( filterDefRegistrations ).hasSize( 2 );

		final FilterDefRegistration amountFilter = filterDefRegistrations.get( "amount_filter" );
		assertThat( amountFilter.getDefaultCondition() ).isEqualTo( "amount = :amount" );
		assertThat( amountFilter.getParameterTypes() ).hasSize( 1 );
		final ClassDetails amountParameterType = amountFilter.getParameterTypes().get( "amount" );
		assertThat( amountParameterType.getClassName() ).isEqualTo( int.class.getName() );

		final FilterDefRegistration nameFilter = filterDefRegistrations.get( "name_filter" );
		assertThat( nameFilter.getDefaultCondition() ).isEqualTo( "name = :name" );
		assertThat( nameFilter.getParameterTypes() ).hasSize( 1 );
		final ClassDetails nameParameterType = nameFilter.getParameterTypes().get( "name" );
		assertThat( nameParameterType.getClassName() ).isEqualTo( String.class.getName() );
	}
}
