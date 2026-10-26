package org.hibernate.orm.tooling.gradle;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import org.gradle.testkit.runner.GradleRunner;
import org.hibernate.engine.spi.Managed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/// Runs the configurations included in the User Guide using the plugin under test.
///
/// @author Steve Ebersole
class BuildToolGuideTests {
	@TempDir
	Path directory;

	@Test
	void compileProcessAndEnhance() throws Exception {
		prepare( "enhancement.gradle" );
		copy( "model/Book.java", directory.resolve( "src/main/java/example/Book.java" ) );
		run( "classes" );
		assertThat( directory.resolve( "build/generated/sources/annotationProcessor/java/main/example/Book_.java" ) )
				.content().contains( "NumericAttribute" );
		assertThat( directory.resolve( "build/classes/java/main/example/Book_.class" ) ).exists();
		try ( var loader = new URLClassLoader( new java.net.URL[] {
				directory.resolve( "build/classes/java/main" ).toUri().toURL()
		}, getClass().getClassLoader() ) ) {
			assertThat( Managed.class.isAssignableFrom( loader.loadClass( "example.Book" ) ) ).isTrue();
		}
	}

	@Test
	void reverseEngineerAndCompile() throws Exception {
		prepare( "reveng.gradle" );
		String url = "jdbc:h2:" + directory.resolve( "guide" ).toAbsolutePath().toString().replace( '\\', '/' );
		try ( var connection = DriverManager.getConnection( url, "sa", "" ); var statement = connection.createStatement() ) {
			statement.execute( "create table BOOK (ID bigint not null primary key, TITLE varchar(255))" );
		}
		var properties = directory.resolve( "src/main/resources/hibernate.properties" );
		Files.createDirectories( properties.getParent() );
		Files.writeString( properties, "hibernate.connection.url=" + url + "\n"
				+ "hibernate.connection.username=sa\nhibernate.connection.password=\nhibernate.default_schema=PUBLIC\n" );
		run( "generateJava", "classes" );
		assertThat( directory.resolve( "build/generated/sources/reveng/example/Book.java" ) ).exists();
		assertThat( directory.resolve( "build/classes/java/main/example/Book.class" ) ).exists();
	}

	private void prepare(String script) throws Exception {
		String build = resource( script ).replace( "{fullVersion}", org.hibernate.Version.getVersionString() );
		// Consume the artifacts published by this test task, without changing the documented repositories.
		build = build.replace( "mavenCentral()", "mavenLocal()\n    mavenCentral()" );
		Files.writeString( directory.resolve( "build.gradle" ), build );
		Files.writeString( directory.resolve( "settings.gradle" ), "rootProject.name = 'build-tool-guide'\n" );
	}

	private void run(String... tasks) {
		var arguments = new java.util.ArrayList<>( java.util.List.of( tasks ) );
		arguments.addAll( java.util.List.of( "--stacktrace", "--no-configuration-cache", "--no-build-cache" ) );
		GradleRunner.create().withProjectDir( directory.toFile() )
				.withPluginClasspath().withGradleVersion( System.getProperty( "gradle.test.version" ) )
				.withDebug( true ).withArguments( arguments ).forwardOutput().build();
	}

	private void copy(String name, Path target) throws Exception {
		Files.createDirectories( target.getParent() );
		Files.writeString( target, resource( name ) );
	}

	private String resource(String name) throws Exception {
		try ( var input = getClass().getResourceAsStream( "/guide/" + name ) ) {
			return new String( java.util.Objects.requireNonNull( input ).readAllBytes(), StandardCharsets.UTF_8 );
		}
	}
}
