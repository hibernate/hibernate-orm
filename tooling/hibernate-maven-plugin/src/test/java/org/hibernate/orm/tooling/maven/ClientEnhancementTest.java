package org.hibernate.orm.tooling.maven;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies client enhancement through the build tool, including alias precedence.
///
/// @author Steve Ebersole
class ClientEnhancementTest {

	@TempDir
	Path directory;

	@Test
	void optionsSelectTheClientPass() throws Exception {
		verify( null, null, false );
		verify( true, null, true );
		verify( null, true, true );
		verify( false, true, false );
		verify( true, false, true );
	}

	private void verify(Boolean client, Boolean legacy, boolean expected) throws Exception {
		final var root = Files.createTempDirectory( directory, "classes" );
		for ( var type : new Class<?>[] { Book.class, Client.class } ) {
			final String resource = type.getName().replace( '.', '/' ) + ".class";
			final var target = root.resolve( resource );
			Files.createDirectories( target.getParent() );
			try ( var input = type.getClassLoader().getResourceAsStream( resource ) ) {
				Files.write( target, input.readAllBytes() );
			}
		}
		run( root, client, legacy );
		final var output = Files.readAllBytes( root.resolve( Client.class.getName().replace( '.', '/' ) + ".class" ) );
		assertEquals( expected, new String( output, StandardCharsets.ISO_8859_1 ).contains( "$$_hibernate_read_title" ) );
	}

	@Entity
	public static class Book {
		@Id
		public long id;
		public String title;
	}

	public static class Client {
		public String read(Book book) {
			return book.title;
		}
	}

	private void run(Path root, Boolean client, Boolean legacy) throws Exception {
		final var mojo = new HibernateEnhancerMojo();
		final var project = new org.apache.maven.project.MavenProject();
		project.setArtifacts( java.util.Set.of() );
		set( mojo, "project", project );
		set( mojo, "classesDirectory", root.toFile() );
		set( mojo, "enableLazyInitialization", true );
		set( mojo, "enableDirtyTracking", true );
		set( mojo, "enableClientEnhancement", client );
		set( mojo, "enableExtendedEnhancement", legacy );
		mojo.execute();
	}

	private static void set(Object target, String name, Object value) throws Exception {
		final var field = target.getClass().getDeclaredField( name );
		field.setAccessible( true );
		field.set( target, value );
	}
}
