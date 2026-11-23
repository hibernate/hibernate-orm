package org.hibernate.orm.test.schemaupdate.sequencegenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/// Oracle-specific sequence clauses supplied through SequenceGenerator options.
/// These tests export scripts without executing DDL or exercising RAC or replay.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.dialect.OracleDialect"),
		@Setting(name = AvailableSettings.ALLOW_METADATA_ON_BOOT, value = "false")
})
@DomainModel(annotatedClasses = {
		OracleSequenceOptionsTest.ScalableEntity.class,
		OracleSequenceOptionsTest.ReplayEntity.class
})
public class OracleSequenceOptionsTest {

	@Test
	@JiraKey("HHH-18143")
	void testScalableSequence(DomainModelScope scope, @TempDir Path directory) throws Exception {
		assertSequence( scope, directory, "scalable_sequence", "scale" );
	}

	@Test
	@JiraKey("HHH-18144")
	void testKeepSequence(DomainModelScope scope, @TempDir Path directory) throws Exception {
		assertSequence( scope, directory, "replay_sequence", "keep" );
	}

	private void assertSequence(DomainModelScope scope, Path directory, String name, String options)
			throws Exception {
		final var metadata = scope.getDomainModel();
		assertInstanceOf( OracleDialect.class, metadata.getDatabase().getDialect() );
		final var script = directory.resolve( "sequences.sql" );
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( script.toString() )
				.setDelimiter( ";" )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );
		final var commands = Files.readAllLines( script ).stream()
				.map( command -> command.trim().toLowerCase( Locale.ROOT ) )
				.filter( command -> command.startsWith( "create sequence " + name + " " ) )
				.toList();
		assertEquals(
				List.of( "create sequence " + name + " start with 1 increment by 1 " + options + ";" ),
				commands
		);
	}

	@Entity(name = "ScalableSequenceEntity")
	public static class ScalableEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scalable")
		@SequenceGenerator(name = "scalable", sequenceName = "scalable_sequence", allocationSize = 1, options = "SCALE")
		private Long id;
	}

	@Entity(name = "ReplaySequenceEntity")
	public static class ReplayEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "replay")
		@SequenceGenerator(name = "replay", sequenceName = "replay_sequence", allocationSize = 1, options = "KEEP")
		private Long id;
	}
}
