package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/// A `columnDefinition` declaring a length other than the `@Column.length` default must not be
/// considered changed by schema update when the table was created from that very definition.
@JiraKey("HHH-19998")
public class ColumnDefinitionLengthSchemaUpdateTest {
	private File output;
	private StandardServiceRegistry ssr;
	private Class<?> entityClass;

	@BeforeEach
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void tearDown() {
		if ( entityClass != null ) {
			new SchemaExport()
					.setHaltOnError( false )
					.setFormat( false )
					.drop( EnumSet.of( TargetType.DATABASE ), buildMetadata() );
		}
		output.delete();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testColumnDefinitionWithLength() throws Exception {
		entityClass = Widget.class;
		assertThat( createThenUpdate() ).isEmpty();
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	public void testGeneratedColumnDefinitionWithLength() throws Exception {
		entityClass = DomainEvent.class;
		assertThat( createThenUpdate() ).isEmpty();
	}

	private String createThenUpdate() throws IOException {
		new SchemaExport()
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE ), buildMetadata() );
		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), buildMetadata() );
		return new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.replace( System.lineSeparator(), "" );
	}

	private MetadataImplementor buildMetadata() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( entityClass )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		return metadata;
	}

	@Entity(name = "Widget")
	public static class Widget {
		@Id
		private Long id;

		@Column(columnDefinition = "varchar(256)")
		private String name;
	}

	@Entity(name = "DomainEvent")
	public static class DomainEvent {
		@Id
		private Long id;

		@Column(columnDefinition = "jsonb")
		private String event;

		@Column(columnDefinition = "varchar(256) generated always as (event ->> 'type') stored")
		private String eventType;
	}
}
