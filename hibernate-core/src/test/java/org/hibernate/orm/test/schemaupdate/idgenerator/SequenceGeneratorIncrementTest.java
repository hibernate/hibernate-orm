/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.idgenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@BaseUnitTest
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceGeneratorIncrementTest {
	private File output;
	private ServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
	}

	@AfterEach
	public void tearDown() {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		ServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSequence() throws Exception {
		buildMetadata( TestEntity.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 50" ) );
	}

	@Test
	public void testSequenceLegacy() throws Exception {
		buildMetadata( TestEntity.class, LegacyNamingStrategy.class.getName() );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 1" ) );
	}

	@Test
	public void testSequenceNoMatchingGenerator() throws Exception {
		buildMetadata( TestEntity2.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 50" ) );
	}

	@Test
	public void testSequenceNoMatchingGeneratorLegacy() throws Exception {
		buildMetadata( TestEntity2.class, LegacyNamingStrategy.class.getName() );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 1" ) );
	}

	@Test
	public void testSequenceGeneratorWithDefaultAllocationSize() throws Exception {
		buildMetadata( TestEntity3.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 50" ) );
	}

	@Test
	public void testSequenceGeneratorWithDefaultAllocationSizeLegacy() throws Exception {
		buildMetadata( TestEntity3.class, LegacyNamingStrategy.class.getName() );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 50" ) );
	}

	@Test
	public void testSequenceGeneratorWithAllocationSize() throws Exception {
		buildMetadata( TestEntity4.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 20" ) );
	}

	@Test
	public void testSequenceGeneratorWithAllocationSizeLegacy() throws Exception {
		buildMetadata( TestEntity4.class, LegacyNamingStrategy.class.getName() );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 20" ) );
	}

	@Test
	public void testSequenceGenericGeneratorWithDefaultAllocationSize() throws Exception {
		buildMetadata( TestEntity5.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 50" ) );
	}

	@Test
	public void testSequenceGenericGeneratorWithDefaultAllocationSizeLegacy() throws Exception {
		buildMetadata( TestEntity5.class, LegacyNamingStrategy.class.getName() );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 1" ) );
	}

	@Test
	public void testSequenceGenericGeneratorWithAllocationSize() throws Exception {
		buildMetadata( TestEntity6.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 10" ) );
	}

	@Test
	public void testSequenceGenericGeneratorWithAllocationSizeLegacy() throws Exception {
		buildMetadata( TestEntity6.class , LegacyNamingStrategy.class.getName());

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 10" ) );
	}

	@Test
	public void testSequenceHbm() throws Exception {
		buildMetadata( "org/hibernate/orm/test/schemaupdate/idgenerator/sequence.hbm.xml" );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 50" ) );
	}

	@Test
	public void testSequenceHbmLegacy() throws Exception {
		buildMetadata(
				"org/hibernate/orm/test/schemaupdate/idgenerator/sequence.hbm.xml",
				LegacyNamingStrategy.class.getName()
		);

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, containsString( "increment by 1" ) );
	}

	private void buildMetadata(Class annotatedClass) {
		buildMetadata( annotatedClass, null, null );
	}

	private void buildMetadata(String hbm) {
		buildMetadata( null, hbm, null );
	}


	private void buildMetadata(Class annotatedClass, String namingStrategy) {
		buildMetadata( annotatedClass, null, namingStrategy );
	}

	private void buildMetadata(String hbm, String namingStrategy) {
		buildMetadata( null, hbm, namingStrategy );
	}

	private void buildMetadata(Class annotatedClass, String hbm, String namingStrategy) {
		StandardServiceRegistryBuilder standardServiceRegistryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		standardServiceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, "false" );

		if ( namingStrategy != null ) {
			standardServiceRegistryBuilder.applySetting(
					AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
					namingStrategy
			);
		}

		ssr = standardServiceRegistryBuilder.build();
		final MetadataSources metadataSources = new MetadataSources( ssr );
		if ( annotatedClass != null ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
		if ( hbm != null ) {
			metadataSources.addResource( hbm );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
	}

	private void createSchema() {
		final SchemaExport schemaExport = new SchemaExport();
		schemaExport.setOutputFile( output.getAbsolutePath() );
		schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity2")
	public static class TestEntity2 {
		@Id
		@GeneratedValue(generator = "no_seq_gen")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity3")
	public static class TestEntity3 {
		@Id
		@GeneratedValue(generator = "seq_gen")
		@SequenceGenerator(name = "seq_gen")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity4")
	public static class TestEntity4 {
		@Id
		@GeneratedValue(generator = "seq_gen")
		@SequenceGenerator(name = "seq_gen", allocationSize = 20)
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity5")
	public static class TestEntity5 {
		@Id
		@GeneratedValue(generator = "ID_GENERATOR")
		@GenericGenerator(name = "ID_GENERATOR", strategy = "enhanced-sequence")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity6")
	public static class TestEntity6 {
		@Id
		@GeneratedValue(generator = "ID_GENERATOR")
		@GenericGenerator(name = "ID_GENERATOR", strategy = "enhanced-sequence", parameters = @org.hibernate.annotations.Parameter(
				name = "increment_size",
				value = "10"
		)
		)
		private Long id;

		private String name;
	}

}
