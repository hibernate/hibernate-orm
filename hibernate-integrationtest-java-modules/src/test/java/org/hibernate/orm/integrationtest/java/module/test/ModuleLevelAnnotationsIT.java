/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.internal.CustomCollectionTypeSemantics;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.orm.integrationtest.java.module.test.annotation.CollectionSubjectEntity;
import org.hibernate.orm.integrationtest.java.module.test.annotation.CompositeSubjectEntity;
import org.hibernate.orm.integrationtest.java.module.test.annotation.ConverterSubjectEntity;
import org.hibernate.orm.integrationtest.java.module.test.annotation.EmbeddableSubjectEntity;
import org.hibernate.orm.integrationtest.java.module.test.annotation.ModuleAnnotationEntity;
import org.hibernate.orm.integrationtest.java.module.test.annotation.StubDomainType;
import org.hibernate.orm.integrationtest.java.module.test.annotation.TypeSubjectEntity;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.CustomType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-20802">HHH-20802</a>
 */
public class ModuleLevelAnnotationsIT {

	@Test
	void testJpaNamedQueryOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getNamedHqlQueryMapping( "moduleQuery" ),
					"Named query 'moduleQuery' defined on module-info.java should be available"
			);
		}
	}

	@Test
	void testJpaNamedNativeQueryOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getNamedNativeQueryMapping( "moduleNativeQuery" ),
					"Named native query 'moduleNativeQuery' defined on module-info.java should be available"
			);
		}
	}

	@Test
	void testHibernateNamedQueryOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getNamedHqlQueryMapping( "hqlModuleQuery" ),
					"Hibernate @NamedQuery 'hqlModuleQuery' defined on module-info.java should be available"
			);
		}
	}

	@Test
	void testHibernateNamedNativeQueryOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getNamedNativeQueryMapping( "hqlModuleNativeQuery" ),
					"Hibernate @NamedNativeQuery 'hqlModuleNativeQuery' defined on module-info.java should be available"
			);
		}
	}

	@Test
	void testFilterDefOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getFilterDefinition( "moduleFilter" ),
					"Filter definition 'moduleFilter' from module-info.java should be available"
			);
		}
	}

	@Test
	void testJpaNamedStoredProcedureQueryOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getNamedProcedureCallMapping( "moduleProc" ),
					"Named stored procedure query 'moduleProc' defined on module-info.java should be available"
			);
		}
	}

	@Test
	void testNamedEntityGraphOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getNamedEntityGraph( "moduleGraph" ),
					"Named entity graph 'moduleGraph' defined on module-info.java should be available"
			);
		}
	}

	@Test
	void testFetchProfileOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					closeableMetadata.metadata.getFetchProfile( "moduleFetchProfile" ),
					"Fetch profile 'moduleFetchProfile' from module-info.java should be available"
			);
		}
	}

	@Test
	void testJavaTypeRegistrationOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			var typeConfig = ((MetadataImplementor) closeableMetadata.metadata).getTypeConfiguration();
			var descriptor = typeConfig.getJavaTypeRegistry().findDescriptor( StubDomainType.class );
			assertNotNull( descriptor,
					"JavaType for StubDomainType from module-info.java should be available" );
			assertThat( descriptor.getClass().getName() ).endsWith( "StubJavaType" );
		}
	}

	@Test
	void testJdbcTypeRegistrationOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ModuleAnnotationEntity.class ) ) {
			assertNotNull(
					((MetadataImplementor) closeableMetadata.metadata).getTypeConfiguration()
							.getJdbcTypeRegistry().findDescriptor( 9999 ),
					"JdbcType registered with code 9999 from module-info.java should be available"
			);
		}
	}

	@Test
	void testConverterRegistrationOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( ConverterSubjectEntity.class ) ) {
			var entityBinding = closeableMetadata.metadata.getEntityBinding(
					ConverterSubjectEntity.class.getName() );
			assertNotNull( entityBinding, "ConverterSubjectEntity should be bound" );
			var property = entityBinding.getProperty( "convertible" );
			var basicValue = (BasicValue) property.getValue();
			var converterDescriptor = basicValue.getJpaAttributeConverterDescriptor();
			assertNotNull( converterDescriptor,
					"Converter from module-info.java should be auto-applied to StubConvertibleType property" );
			assertThat( converterDescriptor.getAttributeConverterClass().getName() ).endsWith( "StubConverter" );
		}
	}

	@Test
	void testTypeRegistrationOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( TypeSubjectEntity.class ) ) {
			var entityBinding = closeableMetadata.metadata.getEntityBinding(
					TypeSubjectEntity.class.getName() );
			assertNotNull( entityBinding, "TypeSubjectEntity should be bound" );
			var property = entityBinding.getProperty( "basic" );
			var basicValue = (BasicValue) property.getValue();
			assertThat( basicValue.getType() ).isInstanceOf( CustomType.class );
			assertThat( ((CustomType<?>) basicValue.getType()).getUserType().getClass().getName() )
					.endsWith( "StubUserType" );
		}
	}

	@Test
	void testCollectionTypeRegistrationOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( CollectionSubjectEntity.class ) ) {
			var entityBinding = closeableMetadata.metadata.getEntityBinding(
					CollectionSubjectEntity.class.getName() );
			assertNotNull( entityBinding, "CollectionSubjectEntity should be bound" );
			var property = entityBinding.getProperty( "elements" );
			var collection = (org.hibernate.mapping.Collection) property.getValue();
			assertThat( collection.getCollectionSemantics() )
					.isInstanceOf( CustomCollectionTypeSemantics.class );
			var semantics = (CustomCollectionTypeSemantics<?, ?>) collection.getCollectionSemantics();
			assertThat( semantics.getCollectionType() ).isInstanceOf( CustomCollectionType.class );
			assertThat( ((CustomCollectionType) semantics.getCollectionType())
					.getUserType().getClass().getName() ).endsWith( "StubUserCollectionType" );
		}
	}

	@Test
	void testCompositeTypeRegistrationOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( CompositeSubjectEntity.class ) ) {
			var entityBinding = closeableMetadata.metadata.getEntityBinding(
					CompositeSubjectEntity.class.getName() );
			assertNotNull( entityBinding, "CompositeSubjectEntity should be bound" );
			var property = entityBinding.getProperty( "composite" );
			assertThat( property.getValue() ).isInstanceOf( Component.class );
		}
	}

	@Test
	void testEmbeddableInstantiatorRegistrationOnModuleInfo() {
		try ( var closeableMetadata = buildModuleMetadata( EmbeddableSubjectEntity.class ) ) {
			var entityBinding = closeableMetadata.metadata.getEntityBinding(
					EmbeddableSubjectEntity.class.getName() );
			assertNotNull( entityBinding, "EmbeddableSubjectEntity should be bound" );
			var property = entityBinding.getProperty( "embeddable" );
			var component = (Component) property.getValue();
			assertNotNull( component.getCustomInstantiator(),
					"Custom instantiator from module-info.java should be registered for StubEmbeddable" );
			assertThat( component.getCustomInstantiator().getName() )
					.endsWith( "StubEmbeddableInstantiator" );
		}
	}

	private CloseableMetadata buildModuleMetadata(Class<?>... entityClasses) {
		var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", "org.hibernate.dialect.H2Dialect" )
				.applySetting( "hibernate.connection.url",
						"jdbc:h2:mem:module_annotations;DB_CLOSE_DELAY=-1" )
				.applySetting( "hibernate.connection.username", "sa" )
				.build();
		var metadataSources = new MetadataSources( serviceRegistry );
		for ( var clazz : entityClasses ) {
			metadataSources.addAnnotatedClass( clazz );
		}
		metadataSources.addModule( getClass().getModule() );
		return new CloseableMetadata( metadataSources.buildMetadata(), serviceRegistry );
	}

	private record CloseableMetadata(Metadata metadata,
									 org.hibernate.service.ServiceRegistry serviceRegistry)
			implements AutoCloseable {
		@Override
		public void close() {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
}
