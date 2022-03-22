package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import static org.junit.jupiter.api.Assertions.assertTrue;


@BaseUnitTest
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceIdGenerationStrategyTest {

	private File output;
	private ServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
	}

	private void buildMetadata(Class annotatedClass) {
		buildMetadata( null, null, null, annotatedClass );
	}

	private void buildMetadata(Integer increment, Class annotatedClass) {
		buildMetadata( null, increment, null, annotatedClass );
	}

	private void buildMetadata(
			String preferSequencePerEntity,
			Integer increment,
			Class annotatedClass) {
		buildMetadata( preferSequencePerEntity, increment, null, annotatedClass );
	}

	private void buildMetadata(
			String preferSequencePerEntity,
			Integer increment,
			String preferGeneratorName,
			Class annotatedClass) {
		StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
		standardServiceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, "false" );

		if ( preferSequencePerEntity != null ) {
			standardServiceRegistryBuilder.applySetting(
					AvailableSettings.PREFER_SEQUENCE_PER_ENTITY,
					preferSequencePerEntity
			);
		}
		if ( preferGeneratorName != null ) {
			standardServiceRegistryBuilder.applySetting(
					AvailableSettings.PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME,
					preferGeneratorName
			);
		}
		if ( increment != null ) {
			standardServiceRegistryBuilder.applySetting(
					AvailableSettings.HIBERNATE_ID_GENERATOR_DEFAULT_INCREMENT_SIZE,
					increment.toString()
			);
		}
		ssr = standardServiceRegistryBuilder.build();
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( annotatedClass );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
	}

	@AfterEach
	public void tearDown() {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		ServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSequenceName() throws Exception {
		buildMetadata( "false", null, TestEntity.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testSequenceName2() throws Exception {
		buildMetadata( TestEntity.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "TestEntity_SEQ" ) );
	}

	@Test
	public void testNoGenerator() throws Exception {
		buildMetadata( TestEntity2.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "table_generator" ) );
	}

	@Test
	public void testNoGenerator2() throws Exception {
		buildMetadata( "false", null, "false", TestEntity2.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testNoGenerator3() throws Exception {
		buildMetadata( "false", null, "false", TestEntity2.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testGeneratorWithoutSequenceName() throws Exception {
		buildMetadata( TestEntity3.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "table_generator" ) );
	}

	@Test
	public void testGeneratorWithoutSequenceName2() throws Exception {
		buildMetadata( "false", null, "false", TestEntity3.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testGeneratorWithSequenceName() throws Exception {
		buildMetadata( "false", null, "false", TestEntity4.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "test_sequence" ) );
	}

	@Test
	public void testGeneratorWithSequenceName2() throws Exception {
		buildMetadata( TestEntity4.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "test_sequence" ) );
	}

	@Test
	public void testSequenceIncrement() throws Exception {
		buildMetadata( TestEntity.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "increment by 50" ) );
	}

	@Test
	public void testSequenceIncrement2() throws Exception {
		buildMetadata( 1, TestEntity.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "increment by 1" ) );
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
		@GeneratedValue(generator = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity3")
	public static class TestEntity3 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@SequenceGenerator(name = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity4")
	public static class TestEntity4 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@SequenceGenerator(name = "table_generator", sequenceName = "test_sequence")
		private Long id;

		private String name;
	}
}
