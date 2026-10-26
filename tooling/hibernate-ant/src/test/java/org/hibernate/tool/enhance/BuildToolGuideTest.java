package org.hibernate.tool.enhance;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.hibernate.engine.spi.Managed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Executes the Ant build files included in the User Guide with the test dependency classpath.
///
/// @author Steve Ebersole
class BuildToolGuideTest {
	@TempDir
	Path directory;

	@Test
	void compilationProcessingAndEnhancementSelection() throws Exception {
		for ( String name : new String[] { "Book", "Excluded" } ) {
			var target = directory.resolve( "src/main/java/example/" + name + ".java" );
			Files.createDirectories( target.getParent() );
			Files.writeString( target, resource( "model/" + name + ".java" ) );
		}
		var project = prepare( "build.xml" );
		project.executeTarget( "enhance" );
		assertTrue( Files.exists( directory.resolve( "target/generated-sources/example/Book_.java" ) ) );
		assertTrue( Files.exists( directory.resolve( "target/classes/example/Book_.class" ) ) );
		try ( var loader = new URLClassLoader( new java.net.URL[] { directory.resolve( "target/classes" ).toUri().toURL() },
				getClass().getClassLoader() ) ) {
			assertTrue( Managed.class.isAssignableFrom( loader.loadClass( "example.Book" ) ) );
			assertFalse( Managed.class.isAssignableFrom( loader.loadClass( "example.Excluded" ) ) );
		}
	}

	@Test
	void generatedEntitiesCompile() throws Exception {
		String url = "jdbc:h2:" + directory.resolve( "guide" ).toAbsolutePath().toString().replace( '\\', '/' );
		try ( var connection = DriverManager.getConnection( url, "sa", "" ); var statement = connection.createStatement() ) {
			statement.execute( "create table BOOK (ID bigint not null primary key, TITLE varchar(255))" );
		}
		var properties = directory.resolve( "src/main/resources/hibernate.properties" );
		Files.createDirectories( properties.getParent() );
		Files.writeString( properties, "hibernate.connection.url=" + url + "\n"
				+ "hibernate.connection.username=sa\nhibernate.connection.password=\nhibernate.default_schema=PUBLIC\n" );
		var project = prepare( "reveng.xml" );
		project.executeTarget( "compile" );
		assertTrue( Files.exists( directory.resolve( "target/generated-sources/example/Book.java" ) ) );
		assertTrue( Files.exists( directory.resolve( "target/classes/example/Book.class" ) ) );
	}

	private Project prepare(String name) throws Exception {
		var file = directory.resolve( name );
		Files.writeString( file, resource( name ) );
		// The test supplies resolved dependencies in place of manually populated library directories.
		Files.createDirectories( directory.resolve( "lib" ) );
		Files.createDirectories( directory.resolve( "processor-lib" ) );
		var project = new Project();
		project.init();
		project.setBaseDir( directory.toFile() );
		ProjectHelper.configureProject( project, file.toFile() );
		var classpath = new org.apache.tools.ant.types.Path( project, System.getProperty( "guide.test.classpath" ) );
		project.addReference( "tooling.path", classpath );
		project.addReference( "processor.path", classpath );
		return project;
	}

	private String resource(String name) throws Exception {
		try ( var input = getClass().getResourceAsStream( "/guide/" + name ) ) {
			return new String( java.util.Objects.requireNonNull( input ).readAllBytes(), StandardCharsets.UTF_8 );
		}
	}
}
