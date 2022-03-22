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

import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class TableIdGenerationStrategyTest {

	private File output;
	private ServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
	}

	private void buildMetadata(String hibernate_sequence, String increment) {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.FORMAT_SQL, "false" )
				.applySetting( AvailableSettings.HIBERNATE_ID_GENERATOR_DEFAULT_INCREMENT_SIZE, increment )
				.applySetting( AvailableSettings.PREFER_SEQUENCE_PER_ENTITY, hibernate_sequence )
				.build();
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( TestEntity.class );
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
		buildMetadata( "hibernate_sequence", "" );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequences" ) );
	}

	@Test
	public void testSequenceName2() throws Exception {
		buildMetadata( "", "" );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequences" ) );
	}

	private void createSchema() {
		final SchemaExport schemaExport = new SchemaExport();
		schemaExport.setOutputFile( output.getAbsolutePath() );
		schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Long id;

		private String name;
	}
}
