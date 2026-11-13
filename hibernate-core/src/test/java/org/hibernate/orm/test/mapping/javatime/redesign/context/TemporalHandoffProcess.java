package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.BasicValue;

import static org.assertj.core.api.Assertions.assertThat;

/// Test-only separate-JVM period phases. Connection settings travel in a private temporary
/// file; the durable handoff manifest contains only explicit, non-secret evidence fields.
/// No JUnit schema cleaner or nested build runs between writer and reader.
///
/// @author Steve Ebersole
public final class TemporalHandoffProcess {
	private TemporalHandoffProcess() {}

	static void periods(Path directory, boolean server) throws Exception {
		Files.createDirectories( directory );
		var config = ContextEnvironment.handoffSettings();
		config.setProperty( "handoff.server", Boolean.toString( server ) );
		config.setProperty( "handoff.manifest", directory.resolve( "manifest.properties" ).toAbsolutePath().toString() );
		config.setProperty( "handoff.source_revision", revision() );
		if ( ContextEnvironment.is( config.getProperty( "hibernate.dialect" ), H2Dialect.class ) ) {
			config.setProperty( "hibernate.connection.url", "jdbc:h2:" + directory.resolve( "periods" ).toAbsolutePath() );
			config.setProperty( "hibernate.connection.username", "sa" );
			config.setProperty( "hibernate.connection.password", "" );
		}
		Path settings = directory.resolve( "connection.properties" );
		Throwable primary = null;
		try {
			phase( config, settings, "write", "America/Denver", "Asia/Tokyo", directory );
			phase( config, settings, "read", "Asia/Tokyo", "America/Denver", directory );
			System.out.println( Files.readString( directory.resolve( "manifest.properties" ) ) );
		}
		catch ( Exception | Error failure ) { primary = failure; throw failure; }
		finally {
			try { phase( config, settings, "cleanup", "UTC", "UTC", directory ); }
			catch ( Exception | Error failure ) { if ( primary != null ) primary.addSuppressed( failure ); else throw failure; }
			finally { Files.deleteIfExists( settings ); }
		}
	}

	static void versions(Path directory, String profile, String kind, boolean database, boolean metadata) throws Exception {
		Files.createDirectories( directory );
		var config = ContextEnvironment.handoffSettings();
		config.setProperty( "handoff.task", kind.equals( "Period" ) ? "period-migration" : "version" );
		config.setProperty( "handoff.profile", profile );
		config.setProperty( "handoff.kind", kind );
		config.setProperty( "handoff.database", Boolean.toString( database ) );
		config.setProperty( "handoff.metadata", Boolean.toString( metadata ) );
		config.setProperty( "handoff.manifest", directory.resolve( "manifest.properties" ).toAbsolutePath().toString() );
		config.setProperty( "handoff.source_revision", revision() );
		if ( ContextEnvironment.is( config.getProperty( "hibernate.dialect" ), H2Dialect.class ) ) {
			config.setProperty( "hibernate.connection.url", "jdbc:h2:" + directory.resolve( "versions" ).toAbsolutePath() );
			config.setProperty( "hibernate.connection.username", "sa" ); config.setProperty( "hibernate.connection.password", "" );
		}
		String zone = profile.equals( "M05" ) ? "UTC" : "America/Denver";
		Path settings = directory.resolve( "connection.properties" );
		Throwable primary = null;
		try {
			String dialect = config.getProperty( "hibernate.dialect" );
			boolean separateJvm = !metadata && switch ( profile ) {
				case "M01" -> database;
				case "M02" -> ContextEnvironment.is( dialect, H2Dialect.class ) && !database;
				case "M03" -> ContextEnvironment.is( dialect, PostgreSQLDialect.class ) && database;
				case "M04" -> ContextEnvironment.is( dialect, OracleDialect.class ) && !database;
				case "M05" -> ContextEnvironment.is( dialect, H2Dialect.class );
				case "M06" -> kind.equals( "Zoned" ) && database;
				default -> false;
			};
			if ( separateJvm ) {
				phase( config, settings, "write", zone, zone, directory );
				phase( config, settings, "read", zone, zone, directory );
			}
			else phase( config, settings, "handoff", zone, zone, directory );
			System.out.println( Files.readString( directory.resolve( "manifest.properties" ) ) );
		}
		catch ( Exception | Error failure ) { primary = failure; throw failure; }
		finally {
			try { phase( config, settings, "cleanup", zone, zone, directory ); }
			catch ( Exception | Error failure ) { if ( primary != null ) primary.addSuppressed( failure ); else throw failure; }
			finally { Files.deleteIfExists( settings ); }
		}
	}

	private static void phase(Properties config, Path settings, String phase, String jvm, String session, Path directory) throws Exception {
		System.out.println( "HANDOFF_OWNER phase=" + phase
				+ " catalog=" + config.getProperty( "hibernate.test.temporal.expected_catalog", "private" )
				+ " schema=" + config.getProperty( "hibernate.test.temporal.expected_schema", "private" ) );
		config.setProperty( "hibernate.test.temporal.jvm_zone", jvm );
		config.setProperty( "hibernate.test.temporal.session_zone", session );
		if ( config.containsKey( "handoff.task" ) ) {
			if ( config.getProperty( "handoff.profile" ).equals( "M03" ) ) config.setProperty( "hibernate.jdbc.time_zone", "UTC" );
			else config.remove( "hibernate.jdbc.time_zone" );
		}
		else config.setProperty( "hibernate.jdbc.time_zone", "Europe/Paris" );
		config.setProperty( "hibernate.connection.init_sql", ContextEnvironment.is( config.getProperty( "hibernate.dialect" ), OracleDialect.class )
				? "alter session set time_zone='" + session + "'" : "set time zone '" + session + "'" );
		try ( var output = Files.newOutputStream( settings ) ) { config.store( output, "Temporary test connection configuration" ); }
		Path log = directory.resolve( phase + ".log" );
		var process = new ProcessBuilder( Path.of( System.getProperty( "java.home" ), "bin", "java" ).toString(),
				"-Duser.timezone=" + jvm, "-ea", "-cp", classpath(), TemporalHandoffProcess.class.getName(),
				settings.toAbsolutePath().toString(), phase ).redirectErrorStream( true ).redirectOutput( log.toFile() ).start();
		try {
			assertThat( process.waitFor( 90, TimeUnit.SECONDS ) ).as( "child phase %s completed; log %s", phase, log ).isTrue();
			if ( process.exitValue() != 0 ) {
				String output = Files.readString( log );
				int exception = output.lastIndexOf( "Exception in thread" );
				if ( exception >= 0 ) System.out.println( output.substring( exception ) );
			}
			assertThat( process.exitValue() ).as( "child phase %s; log %s", phase, log ).isZero();
			assertThat( Files.readString( log ) ).as( "child success marker" ).contains( "HANDOFF_SUCCESS " + phase );
			try ( var lines = Files.lines( log ) ) {
				lines.filter( line -> line.startsWith( "PERIOD_" ) || line.startsWith( "CONTEXT_BOOT" )
						|| line.startsWith( "HANDOFF_SUCCESS" ) || line.startsWith( "MIGRATION_" ) ).forEach( System.out::println );
			}
		}
		finally { if ( process.isAlive() ) process.destroyForcibly().waitFor(); }
	}

	private static String classpath() throws Exception {
		var entries = new LinkedHashSet<String>( List.of( System.getProperty( "java.class.path" ).split( File.pathSeparator ) ) );
		for ( var loader = TemporalHandoffProcess.class.getClassLoader(); loader != null; loader = loader.getParent() ) {
			if ( loader instanceof URLClassLoader urls ) {
				for ( var url : urls.getURLs() ) if ( url.getProtocol().equals( "file" ) ) entries.add( Path.of( url.toURI() ).toString() );
			}
		}
		return String.join( File.pathSeparator, entries );
	}

	private static String revision() throws Exception {
		var process = new ProcessBuilder( "git", "rev-parse", "HEAD" ).redirectErrorStream( true ).start();
		assertThat( process.waitFor( 10, TimeUnit.SECONDS ) ).isTrue();
		assertThat( process.exitValue() ).isZero();
		return new String( process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8 ).trim();
	}

	public static void main(String[] args) throws Exception {
		var settings = new Properties();
		try ( var input = Files.newInputStream( Path.of( args[0] ) ) ) { settings.load( input ); }
		settings.forEach( (key, value) -> System.setProperty( (String) key, (String) value ) );
		ContextEnvironment.inheritVersion( settings.getProperty( "jakarta.persistence.database-product-version" ) );
		assertThat( java.time.ZoneId.systemDefault() ).isEqualTo(
				java.time.ZoneId.of( settings.getProperty( "hibernate.test.temporal.jvm_zone" ) ) );
		if ( "period-migration".equals( settings.getProperty( "handoff.task" ) ) ) {
			var fixture = new TemporalPeriodMigrationFixture( settings.getProperty( "handoff.profile" ),
					Boolean.parseBoolean( settings.getProperty( "handoff.database" ) ), Boolean.parseBoolean( settings.getProperty( "handoff.metadata" ) ) );
			Path path = Path.of( settings.getProperty( "handoff.manifest" ) );
			if ( args[1].equals( "write" ) || args[1].equals( "handoff" ) ) {
				var evidence = fixture.write();
				evidence.setProperty( "java.runtime", System.getProperty( "java.runtime.version" ) );
				evidence.setProperty( "timezone.rules", java.time.zone.ZoneRulesProvider.getVersions( "America/Denver" ).lastKey() );
				evidence.setProperty( "executing.revision", settings.getProperty( "handoff.source_revision" ) );
				evidence.setProperty( "zones", "JVM=" + java.time.ZoneId.systemDefault() + " session=" + settings.getProperty( "hibernate.test.temporal.session_zone" ) + " JDBC=unset" );
				try ( var output = Files.newOutputStream( path ) ) { evidence.store( output, "Period migration fixture" ); }
				if ( args[1].equals( "handoff" ) ) {
					fixture.read( evidence );
					try ( var output = Files.newOutputStream( path ) ) { evidence.store( output, "Completed migration handoff" ); }
				}
			}
			else if ( args[1].equals( "read" ) ) {
				var evidence = new Properties();
				try ( var input = Files.newInputStream( path ) ) { evidence.load( input ); }
				fixture.read( evidence );
				try ( var output = Files.newOutputStream( path ) ) { evidence.store( output, "Completed migration handoff" ); }
			}
			else fixture.cleanup();
			System.out.println( "HANDOFF_SUCCESS " + args[1] ); return;
		}
		if ( "version".equals( settings.getProperty( "handoff.task" ) ) ) {
			var fixture = new TemporalVersionMigrationFixture( settings.getProperty( "handoff.profile" ),
					settings.getProperty( "handoff.kind" ), Boolean.parseBoolean( settings.getProperty( "handoff.database" ) ),
					Boolean.parseBoolean( settings.getProperty( "handoff.metadata" ) ) );
			Path path = Path.of( settings.getProperty( "handoff.manifest" ) );
			if ( args[1].equals( "write" ) || args[1].equals( "handoff" ) ) {
				var evidence = fixture.write();
				evidence.setProperty( "java.runtime", System.getProperty( "java.runtime.version" ) );
				evidence.setProperty( "timezone.rules", java.time.zone.ZoneRulesProvider.getVersions( "America/Denver" ).lastKey() );
				evidence.setProperty( "executing.revision", settings.getProperty( "handoff.source_revision" ) );
				evidence.setProperty( "zones", "JVM=America/Denver session=America/Denver JDBC=" + settings.getProperty( "hibernate.jdbc.time_zone", "unset" ) );
				try ( var output = Files.newOutputStream( path ) ) { evidence.store( output, "Version migration fixture" ); }
				if ( args[1].equals( "handoff" ) ) {
					fixture.read( evidence );
					try ( var output = Files.newOutputStream( path ) ) { evidence.store( output, "Completed migration handoff" ); }
				}
			}
			else if ( args[1].equals( "read" ) ) {
				var evidence = new Properties();
				try ( var input = Files.newInputStream( path ) ) { evidence.load( input ); }
				fixture.read( evidence );
				try ( var output = Files.newOutputStream( path ) ) { evidence.store( output, "Completed migration handoff" ); }
			}
			else fixture.cleanup();
			System.out.println( "HANDOFF_SUCCESS " + args[1] ); return;
		}
		boolean server = Boolean.parseBoolean( settings.getProperty( "handoff.server" ) );
		String phase = args[1];
		var bootstrap = TemporalPeriodFixture.settings( true, "HISTORY_TABLE", server, new TemporalPeriodFixture.Fixed() );
		if ( phase.equals( "write" ) || phase.equals( "cleanup" ) ) {
			try ( var scope = ContextScope.boot( false, false, bootstrap, TemporalPeriodFixture.Period.class ) ) {
				scope.provider.blocked = false;
				if ( phase.equals( "cleanup" ) ) scope.drop();
				else scope.factory.getSchemaManager().create( false );
			}
		}
		if ( !phase.equals( "cleanup" ) ) {
			Path path = Path.of( settings.getProperty( "handoff.manifest" ) );
			var manifest = new Properties();
			try ( var fixture = new TemporalPeriodFixture( false, "HISTORY_TABLE", server,
					TemporalPeriodFixture.Period.class, true, false, java.util.Map.of() ) ) {
				if ( phase.equals( "write" ) ) {
					var points = fixture.lifecycle( 1, TemporalPeriodFixture.FOLD );
					for ( int i = 0; i < points.length; i++ ) manifest.setProperty( "boundary." + i, points[i].toString() );
					var column = fixture.scope.metadata.getEntityBinding( TemporalPeriodFixture.Period.class.getName() ).getAuxiliaryColumn( "rowStart" );
					var jdbc = ( (BasicValue) column.getValue() ).resolve().getJdbcType();
					manifest.setProperty( "descriptor", jdbc.getClass().getName() );
					manifest.setProperty( "jdbc_code", Integer.toString( jdbc.getJdbcTypeCode() ) );
					manifest.setProperty( "ddl", column.getSqlType( fixture.scope.metadata ) );
					manifest.setProperty( "precision", Integer.toString( fixture.precision ) );
					manifest.setProperty( "raw.entity", fixture.intervals( false, 1 ).toString() );
					manifest.setProperty( "raw.collection", fixture.intervals( true, 1 ).toString() );
					manifest.setProperty( "database", fixture.writer.getMetaData().getDatabaseProductVersion() );
					manifest.setProperty( "driver", fixture.writer.getMetaData().getDriverVersion() );
					manifest.setProperty( "source_revision", settings.getProperty( "handoff.source_revision" ) );
					manifest.setProperty( "source", "current-runtime preserving=true HISTORY_TABLE server=" + server );
					manifest.setProperty( "writer.zones", "JVM=America/Denver session=Asia/Tokyo JDBC=Europe/Paris" );
					manifest.setProperty( "reader.zones", "JVM=Asia/Tokyo session=America/Denver JDBC=Europe/Paris" );
					manifest.setProperty( "java.runtime", System.getProperty( "java.runtime.version" ) );
					try ( var output = Files.newOutputStream( path ) ) { manifest.store( output, "Owned period handoff evidence; no connection credentials" ); }
				}
				else {
					try ( var input = Files.newInputStream( path ) ) { manifest.load( input ); }
					fixture.assertHistory( 1, Instant.parse( manifest.getProperty( "boundary.0" ) ),
							Instant.parse( manifest.getProperty( "boundary.1" ) ), Instant.parse( manifest.getProperty( "boundary.2" ) ) );
				}
			}
		}
		System.out.println( "HANDOFF_SUCCESS " + phase );
	}
}
