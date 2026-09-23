/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.enhance;

import java.nio.charset.StandardCharsets;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies Gradle's client option and compatibility alias using real enhancement.
///
/// @author Steve Ebersole
class ClientEnhancementTest {

	@TempDir
	java.nio.file.Path directory;

	@Test
	void filePassesRespectSelectionAndSupportClientOnlyEnhancement() throws Exception {
		final var project = ProjectBuilder.builder().build();
		project.getPlugins().apply( "java" );
		project.getPlugins().apply( "org.hibernate.orm" );
		final var orm = project.getExtensions().getByType( HibernateOrmSpec.class );
		orm.enhancement( options -> {
			options.getEnableClientEnhancement().set( true );
			options.getClassNames().set( java.util.List.of( Client.class.getName() ) );
		} );
		for ( var type : java.util.List.of( Book.class, Client.class ) ) {
			final var file = directory.resolve( type.getName().replace( '.', '/' ) + ".class" );
			java.nio.file.Files.createDirectories( file.getParent() );
			java.nio.file.Files.write( file, bytes( type ) );
		}
		final var root = project.getObjects().directoryProperty();
		root.fileValue( directory.toFile() );
		final var loader = new ClassLoader( getClass().getClassLoader() ) {
			@Override
			public java.io.InputStream getResourceAsStream(String name) {
				final var file = directory.resolve( name );
				try {
					return java.nio.file.Files.isRegularFile( file )
							? java.nio.file.Files.newInputStream( file ) : super.getResourceAsStream( name );
				}
				catch (java.io.IOException e) {
					throw new java.io.UncheckedIOException( e );
				}
			}
		};
		final var clientFile = directory.resolve( Client.class.getName().replace( '.', '/' ) + ".class" );
		EnhancementHelper.enhance( root, loader, orm );
		assertThat( java.nio.file.Files.readAllBytes( clientFile ) ).isEqualTo( bytes( Client.class ) );
		final var options = orm.getEnhancement().get();
		options.getClassNames().set( java.util.List.of( Book.class.getName(), Client.class.getName() ) );
		EnhancementHelper.enhance( root, loader, orm );
		assertThat( new String( java.nio.file.Files.readAllBytes( clientFile ), StandardCharsets.ISO_8859_1 ) )
				.contains( "$$_hibernate_read_title" );
		java.nio.file.Files.write( clientFile, bytes( Client.class ) );
		options.getEnableDirtyTracking().set( false );
		options.getEnableLazyInitialization().set( false );
		EnhancementHelper.enhance( root, loader, orm );
		assertThat( new String( java.nio.file.Files.readAllBytes( clientFile ), StandardCharsets.ISO_8859_1 ) )
				.contains( "$$_hibernate_read_title" );
	}

	@Test
	void optionsSelectTheClientPass() throws Exception {
		verify( null, null, false );
		verify( true, null, true );
		verify( null, true, true );
		verify( false, true, false );
		verify( true, false, true );
	}

	private void verify(Boolean client, Boolean legacy, boolean expected) throws Exception {
		final var project = ProjectBuilder.builder().build();
		final var options = project.getObjects().newInstance( EnhancementSpec.class );
		if ( client != null ) {
			options.getEnableClientEnhancement().set( client );
		}
		if ( legacy != null ) {
			options.getEnableExtendedEnhancement().set( legacy );
		}
		final var enhancer = EnhancementHelper.generateEnhancer( getClass().getClassLoader(), options );
		enhancer.discoverTypes( Book.class.getName(), bytes( Book.class ) );
		enhancer.discoverTypes( Client.class.getName(), bytes( Client.class ) );
		assertThat( enhancer.enhance( Book.class.getName(), bytes( Book.class ) ) ).isNotNull();
		final var result = enhancer.enhance( Client.class.getName(), bytes( Client.class ) );
		if ( expected ) {
			assertThat( result ).isNotNull();
			assertThat( new String( result, StandardCharsets.ISO_8859_1 ) ).contains( "$$_hibernate_read_title" );
		}
		else {
			assertThat( result ).isNull();
		}
	}

	private static byte[] bytes(Class<?> type) throws Exception {
		try ( var input = type.getClassLoader().getResourceAsStream( type.getName().replace( '.', '/' ) + ".class" ) ) {
			return input.readAllBytes();
		}
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
}
