package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey("HHH-20850")
public class EnumWithLengthSchemaUpdateTest {
	private File output;
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void tearDown() {
		new SchemaExport()
				.setHaltOnError( false )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), buildMetadata() );
		output.delete();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testUpdateIsNotExecuted() throws Exception {
		new SchemaExport()
				.setHaltOnError( false )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE ), buildMetadata() );
		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), buildMetadata() );

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.replace( System.lineSeparator(), "" );
		assertThat( fileContent ).isEmpty();
	}

	private MetadataImplementor buildMetadata() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Widget.class )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		return metadata;
	}

	@Entity(name = "Widget")
	public static class Widget {
		@Id
		private Long id;

		@Enumerated(EnumType.STRING)
		@Column(nullable = false, length = 20)
		private Status status;
	}

	public enum Status {
		ARCHIVED,
		DRAFT,
		PUBLISHED
	}
}
