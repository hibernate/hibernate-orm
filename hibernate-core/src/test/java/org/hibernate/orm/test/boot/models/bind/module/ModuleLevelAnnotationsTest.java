/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.internal.CustomCollectionTypeSemantics;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.CustomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jira("https://hibernate.atlassian.net/browse/HHH-20802")
public class ModuleLevelAnnotationsTest {

	private static final String MODULE_NAME = "test.module.subject";

	private static final String SUBJECT_PACKAGE = "org.hibernate.orm.test.boot.models.bind.module.subject";

	@Entity(name = "ModuleEntity")
	public static class ModuleEntity {
		@Id
		public Long id;
	}

	@Test
	void testJpaNamedQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@jakarta.persistence.NamedQuery(name = \"moduleQuery\", query = \"select e from ModuleEntity e\")",
				metadata -> assertNotNull(
						metadata.getNamedHqlQueryMapping( "moduleQuery" ),
						"Named query 'moduleQuery' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testJpaNamedNativeQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@jakarta.persistence.NamedNativeQuery(name = \"moduleNativeQuery\", query = \"SELECT 1\")",
				metadata -> assertNotNull(
						metadata.getNamedNativeQueryMapping( "moduleNativeQuery" ),
						"Named native query 'moduleNativeQuery' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testHibernateNamedQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.NamedQuery(name = \"hqlModuleQuery\", query = \"select e from ModuleEntity e\")",
				metadata -> assertNotNull(
						metadata.getNamedHqlQueryMapping( "hqlModuleQuery" ),
						"Hibernate @NamedQuery 'hqlModuleQuery' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testFilterDefOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.FilterDef(name = \"moduleFilter\")",
				metadata -> assertNotNull(
						metadata.getFilterDefinition( "moduleFilter" ),
						"Filter definition 'moduleFilter' from module-info.java should be available"
				)
		);
	}

	@Test
	void testJpaNamedStoredProcedureQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@jakarta.persistence.NamedStoredProcedureQuery(name = \"moduleProc\", procedureName = \"my_procedure\")",
				metadata -> assertNotNull(
						metadata.getNamedProcedureCallMapping( "moduleProc" ),
						"Named stored procedure query 'moduleProc' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testHibernateNamedNativeQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.NamedNativeQuery(name = \"hqlModuleNativeQuery\", query = \"SELECT 1\")",
				metadata -> assertNotNull(
						metadata.getNamedNativeQueryMapping( "hqlModuleNativeQuery" ),
						"Hibernate @NamedNativeQuery 'hqlModuleNativeQuery' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testNamedEntityGraphOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.NamedEntityGraph(name = \"moduleGraph\", graph = \"ModuleEntity: id\")",
				metadata -> assertNotNull(
						metadata.getNamedEntityGraph( "moduleGraph" ),
						"Named entity graph 'moduleGraph' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testFetchProfileOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.FetchProfile(name = \"moduleFetchProfile\")",
				metadata -> assertNotNull(
						metadata.getFetchProfile( "moduleFetchProfile" ),
						"Fetch profile 'moduleFetchProfile' from module-info.java should be available"
				)
		);
	}

	@Test
	void testJavaTypeRegistrationOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.JavaTypeRegistration(javaType = " + SUBJECT_PACKAGE + ".StubDomainType.class,"
				+ " descriptorClass = " + SUBJECT_PACKAGE + ".StubJavaType.class)",
				List.of( SUBJECT_PACKAGE ),
				(metadata, loaded) -> {
					var stubDomainType = loaded.classLoader()
							.loadClass( SUBJECT_PACKAGE + ".StubDomainType" );
					var typeConfig = ((MetadataImplementor) metadata).getTypeConfiguration();
					var descriptor = typeConfig.getJavaTypeRegistry().findDescriptor( stubDomainType );
					assertNotNull( descriptor,
							"JavaType for StubDomainType from module-info.java should be available" );
					assertThat( descriptor.getClass().getName() )
							.isEqualTo( SUBJECT_PACKAGE + ".StubJavaType" );
				}
		);
	}

	@Test
	void testJdbcTypeRegistrationOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.JdbcTypeRegistration(value = org.hibernate.type.descriptor.jdbc.VarcharJdbcType.class,"
				+ " registrationCode = 9999)",
				metadata -> assertNotNull(
						((MetadataImplementor) metadata).getTypeConfiguration()
								.getJdbcTypeRegistry().findDescriptor( 9999 ),
						"JdbcType registered with code 9999 from module-info.java should be available"
				)
		);
	}

	@Test
	void testConverterRegistrationOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.ConverterRegistration(converter = " + SUBJECT_PACKAGE + ".StubConverter.class)",
				List.of( SUBJECT_PACKAGE ),
				List.of( "SubjectEntity" ),
				(metadata, loaded) -> {
					var entityBinding = metadata.getEntityBinding( SUBJECT_PACKAGE + ".SubjectEntity" );
					assertNotNull( entityBinding, "SubjectEntity should be bound" );
					var property = entityBinding.getProperty( "domainType" );
					var basicValue = (BasicValue) property.getValue();
					var converterDescriptor = basicValue.getJpaAttributeConverterDescriptor();
					assertNotNull( converterDescriptor,
							"Converter from module-info.java should be auto-applied to StubDomainType property" );
					assertThat( converterDescriptor.getAttributeConverterClass().getName() )
							.isEqualTo( SUBJECT_PACKAGE + ".StubConverter" );
				}
		);
	}

	@Test
	void testTypeRegistrationOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.TypeRegistration(basicClass = " + SUBJECT_PACKAGE + ".StubDomainType.class,"
				+ " userType = " + SUBJECT_PACKAGE + ".StubUserType.class)",
				List.of( SUBJECT_PACKAGE ),
				List.of( "SubjectEntity" ),
				(metadata, loaded) -> {
					var entityBinding = metadata.getEntityBinding( SUBJECT_PACKAGE + ".SubjectEntity" );
					assertNotNull( entityBinding, "SubjectEntity should be bound" );
					var property = entityBinding.getProperty( "domainType" );
					var basicValue = (BasicValue) property.getValue();
					assertThat( basicValue.getType() ).isInstanceOf( CustomType.class );
					assertThat( ((CustomType<?>) basicValue.getType()).getUserType().getClass().getName() )
							.isEqualTo( SUBJECT_PACKAGE + ".StubUserType" );
				}
		);
	}

	@Test
	void testCollectionTypeRegistrationOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.CollectionTypeRegistration("
				+ "classification = org.hibernate.metamodel.CollectionClassification.BAG,"
				+ " type = " + SUBJECT_PACKAGE + ".StubUserCollectionType.class)",
				List.of( SUBJECT_PACKAGE ),
				List.of( "CollectionSubjectEntity" ),
				(metadata, loaded) -> {
					var entityBinding = metadata.getEntityBinding( SUBJECT_PACKAGE + ".CollectionSubjectEntity" );
					assertNotNull( entityBinding, "CollectionSubjectEntity should be bound" );
					var property = entityBinding.getProperty( "elements" );
					var collection = (org.hibernate.mapping.Collection) property.getValue();
					assertThat( collection.getCollectionSemantics() )
							.isInstanceOf( CustomCollectionTypeSemantics.class );
					var semantics = (CustomCollectionTypeSemantics<?, ?>) collection.getCollectionSemantics();
					assertThat( semantics.getCollectionType() ).isInstanceOf( CustomCollectionType.class );
					assertThat( ((CustomCollectionType) semantics.getCollectionType())
							.getUserType().getClass().getName() )
							.isEqualTo( SUBJECT_PACKAGE + ".StubUserCollectionType" );
				}
		);
	}

	@Test
	void testCompositeTypeRegistrationOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.CompositeTypeRegistration(embeddableClass = " + SUBJECT_PACKAGE + ".StubDomainType.class,"
				+ " userType = " + SUBJECT_PACKAGE + ".StubCompositeUserType.class)",
				List.of( SUBJECT_PACKAGE ),
				List.of( "SubjectEntity" ),
				(metadata, loaded) -> {
					var entityBinding = metadata.getEntityBinding( SUBJECT_PACKAGE + ".SubjectEntity" );
					assertNotNull( entityBinding, "SubjectEntity should be bound" );
					var property = entityBinding.getProperty( "domainType" );
					assertThat( property.getValue() ).isInstanceOf( Component.class );
				}
		);
	}

	@Test
	void testEmbeddableInstantiatorRegistrationOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.EmbeddableInstantiatorRegistration(embeddableClass = " + SUBJECT_PACKAGE + ".StubEmbeddable.class,"
				+ " instantiator = " + SUBJECT_PACKAGE + ".StubEmbeddableInstantiator.class)",
				List.of( SUBJECT_PACKAGE ),
				List.of( "EmbeddableSubjectEntity" ),
				(metadata, loaded) -> {
					var entityBinding = metadata.getEntityBinding( SUBJECT_PACKAGE + ".EmbeddableSubjectEntity" );
					assertNotNull( entityBinding, "EmbeddableSubjectEntity should be bound" );
					var property = entityBinding.getProperty( "embeddable" );
					var component = (Component) property.getValue();
					assertNotNull( component.getCustomInstantiator(),
							"Custom instantiator from module-info.java should be registered for StubEmbeddable" );
					assertThat( component.getCustomInstantiator().getName() )
							.isEqualTo( SUBJECT_PACKAGE + ".StubEmbeddableInstantiator" );
				}
		);
	}

	@Test
	void testGenericGeneratorOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.GenericGenerator(type = org.hibernate.id.IncrementGenerator.class)",
				List.of( SUBJECT_PACKAGE ),
				List.of( "GeneratorSubjectEntity" ),
				(metadata, loaded) -> {
					var entityBinding = metadata.getEntityBinding(
							SUBJECT_PACKAGE + ".GeneratorSubjectEntity" );
					assertNotNull( entityBinding, "GeneratorSubjectEntity should be bound" );
					var idValue = (SimpleValue) entityBinding.getIdentifier();
					assertNotNull( idValue.getCustomIdGeneratorCreator(),
							"@GenericGenerator from module-info.java should configure the id generator" );
				}
		);
	}

	@Test
	void testNativeGeneratorOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@org.hibernate.annotations.NativeGenerator",
				List.of( SUBJECT_PACKAGE ),
				List.of( "GeneratorSubjectEntity" ),
				(metadata, loaded) -> {
					var entityBinding = metadata.getEntityBinding(
							SUBJECT_PACKAGE + ".GeneratorSubjectEntity" );
					assertNotNull( entityBinding, "GeneratorSubjectEntity should be bound" );
					var idValue = (SimpleValue) entityBinding.getIdentifier();
					assertNotNull( idValue.getCustomIdGeneratorCreator(),
							"@NativeGenerator from module-info.java should configure the id generator" );
				}
		);
	}

	private void assertModuleAnnotation(
			Path tempDir,
			String annotation,
			MetadataAssertion assertion) throws Exception {
		assertModuleAnnotation( tempDir, annotation, List.of(), (metadata, loaded) -> assertion.verify( metadata ) );
	}

	private void assertModuleAnnotation(
			Path tempDir,
			String annotation,
			List<String> classPackages,
			ModuleMetadataAssertion assertion) throws Exception {
		assertModuleAnnotation( tempDir, annotation, classPackages, List.of(), assertion );
	}

	private void assertModuleAnnotation(
			Path tempDir,
			String annotation,
			List<String> classPackages,
			List<String> moduleEntitySimpleNames,
			ModuleMetadataAssertion assertion) throws Exception {
		final var opens = classPackages.stream()
				.map( pkg -> "\topens " + pkg + ";\n" )
				.reduce( "", String::concat );
		final var moduleInfoSource = """
				%s
				module %s {
					requires static jakarta.persistence;
					requires static org.hibernate.orm.core;
				%s}
				""".formatted( annotation, MODULE_NAME, opens );

		final var loaded = TestModuleUtil.compileAndLoadModule(
				tempDir, MODULE_NAME, moduleInfoSource, classPackages
		);

		final var bootstrapRegistry = new BootstrapServiceRegistryBuilder()
				.applyClassLoader( loaded.classLoader() )
				.build();
		final var serviceRegistry = new StandardServiceRegistryBuilder( bootstrapRegistry ).build();
		try {
			final var metadataSources = new MetadataSources( serviceRegistry );
			metadataSources.addAnnotatedClass( ModuleEntity.class );
			for ( String simpleClassName : moduleEntitySimpleNames ) {
				metadataSources.addAnnotatedClass(
						loaded.classLoader().loadClass( SUBJECT_PACKAGE + "." + simpleClassName )
				);
			}
			metadataSources.addModule( loaded.module() );

			// Pre-register the module in the ModuleDetailsRegistry, because
			// resolveModuleDetails(String) only searches ModuleLayer.boot()
			// and cannot find modules from custom module layers.
			final var builder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder();
			builder.getBootstrapContext().getModelsContext()
					.getModuleDetailsRegistry().resolveModuleDetails( loaded.module() );

			assertion.verify( builder.build(), loaded );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@FunctionalInterface
	interface MetadataAssertion {
		void verify(Metadata metadata);
	}

	@FunctionalInterface
	interface ModuleMetadataAssertion {
		void verify(Metadata metadata, TestModuleUtil.LoadedModule loaded) throws Exception;
	}
}
