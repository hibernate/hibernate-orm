package org.hibernate.orm.tooling.maven;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import org.hibernate.engine.spi.Managed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Compiles and runs the complete Maven configurations included in the User Guide.
///
/// @author Steve Ebersole
class BuildToolGuideTestIT extends AbstractMavenTestIT {
	@TempDir
	Path directory;

	@Test
	void compilationProcessingAndSelectedEnhancement() throws Exception {
		prepare( "enhance" );
		for ( String name : new String[] { "Book", "Excluded" } ) {
			var target = directory.resolve( "src/main/java/example/" + name + ".java" );
			Files.createDirectories( target.getParent() );
			Files.writeString( target, resource( "model/" + name + ".java" ) );
		}
		runMaven( directory.toString(), "process-classes", "-B" );
		assertTrue( Files.exists( directory.resolve( "target/generated-sources/annotations/example/Book_.java" ) ) );
		assertTrue( Files.exists( directory.resolve( "target/classes/example/Book_.class" ) ) );
		try ( var loader = new URLClassLoader( new java.net.URL[] { directory.resolve( "target/classes" ).toUri().toURL() },
				getClass().getClassLoader() ) ) {
			assertTrue( Managed.class.isAssignableFrom( loader.loadClass( "example.Book" ) ) );
			assertFalse( Managed.class.isAssignableFrom( loader.loadClass( "example.Excluded" ) ) );
		}
	}

	@Test
	void reverseEngineeredSourcesParticipateInCompilation() throws Exception {
		prepare( "reveng" );
		String url = "jdbc:h2:" + directory.resolve( "guide" ).toAbsolutePath().toString().replace( '\\', '/' );
		try ( var connection = DriverManager.getConnection( url, "sa", "" ); var statement = connection.createStatement() ) {
			statement.execute( "create table BOOK (ID bigint not null primary key, TITLE varchar(255))" );
		}
		var properties = directory.resolve( "src/main/resources/hibernate.properties" );
		Files.createDirectories( properties.getParent() );
		Files.writeString( properties, "hibernate.connection.url=" + url + "\n"
				+ "hibernate.connection.username=sa\nhibernate.connection.password=\nhibernate.default_schema=PUBLIC\n" );
		runMaven( directory.toString(), "compile", "-B" );
		assertTrue( Files.exists( directory.resolve( "target/generated-sources/reveng/example/Book.java" ) ) );
		assertTrue( Files.exists( directory.resolve( "target/classes/example/Book.class" ) ) );
	}

	private void prepare(String example) throws Exception {
		Files.writeString( directory.resolve( "pom.xml" ), resource( example + "/pom.xml" )
				.replace( "{fullVersion}", System.getenv( "hibernateVersion" ) ) );
	}

	private String resource(String path) throws Exception {
		try ( var input = getClass().getResourceAsStream( "/guide/" + path ) ) {
			return new String( java.util.Objects.requireNonNull( input ).readAllBytes(), StandardCharsets.UTF_8 );
		}
	}
}
