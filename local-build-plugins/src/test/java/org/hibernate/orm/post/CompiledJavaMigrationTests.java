/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.CONSTRUCTOR_REMOVED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.DECLARED_EXCEPTION_ADDED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.DECLARED_EXCEPTION_REMOVED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.GENERIC_SIGNATURE_CHANGED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.METHOD_REMOVED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.OVERLOAD_ADDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Compare analyzer findings with actual source compilation and binary linkage.
/// These tests intentionally complement the declaration-shape and policy tests.
///
/// @author Steve Ebersole
class CompiledJavaMigrationTests {
	@TempDir
	Path directory;

	@Test
	void inheritedMethodRemainsSourceAndBinaryCompatible() throws Exception {
		final String parent = "package fixture; public class Parent { public String value() { return \"ok\"; } }";
		final Path old = compile( "old", null, Map.of(
				"Parent", parent,
				"Child", "package fixture; public class Child extends Parent { public String value() { return \"ok\"; } }"
		) );
		final Path current = compile( "current", null, Map.of(
				"Parent", parent, "Child", "package fixture; public class Child extends Parent {}"
		) );
		final Map<String, String> client = Map.of( "Client",
				"package fixture; public class Client { public static String run() { return new Child().value(); } }" );
		final Path oldClient = compile( "client", old, client );
		compile( "recompiled", current, client );
		try ( URLClassLoader loader = new URLClassLoader(
				new java.net.URL[] { oldClient.toUri().toURL(), current.toUri().toURL() }, null ) ) {
			assertEquals( "ok", loader.loadClass( "fixture.Client" ).getMethod( "run" ).invoke( null ) );
		}
		assertTrue( new JavaMigrationCompatibilityAnalyzer().analyze(
				List.of( old.toFile() ), List.of( current.toFile() ), List.of( "type:fixture.Child" )
		).getChanges().isEmpty() );
	}

	@Test
	void inheritedCovariantBridgePreservesExistingBinaryClients() throws Exception {
		final String root = "package fixture; public interface Root<T> { T value(); }";
		final String parent = "package fixture; public class Parent implements Root<String> { public String value() { return \"ok\"; } }";
		final Path old = compile( "old", null, Map.of( "Root", root, "Parent", parent, "Child",
				"package fixture; public class Child extends Parent { public String value() { return \"ok\"; } }" ) );
		final Path current = compile( "current", null, Map.of( "Root", root, "Parent", parent, "Child",
				"package fixture; public class Child extends Parent {}" ) );
		final Map<String, String> client = Map.of( "Client",
				"package fixture; public class Client { public static String run() { Child child = new Child(); Root<?> root = child; return child.value() + root.value(); } }" );
		final Path oldClient = compile( "client", old, client );
		compile( "recompiled", current, client );
		try ( URLClassLoader loader = new URLClassLoader(
				new java.net.URL[] { oldClient.toUri().toURL(), current.toUri().toURL() }, null ) ) {
			assertEquals( "okok", loader.loadClass( "fixture.Client" ).getMethod( "run" ).invoke( null ) );
		}
		assertTrue( causes( old, current ).isEmpty() );
	}

	@Test
	void inheritedDefaultMethodRemainsAvailable() throws Exception {
		final String parent = "package fixture; public interface Parent { default String value() { return \"ok\"; } }";
		final Path old = compile( "old", null, Map.of( "Parent", parent, "Child",
				"package fixture; public interface Child extends Parent { default String value() { return \"ok\"; } }" ) );
		final Path current = compile( "current", null, Map.of( "Parent", parent, "Child",
				"package fixture; public interface Child extends Parent {}" ) );
		final Map<String, String> client = Map.of( "Client",
				"package fixture; public class Client { public static String run() { return new Child() {}.value(); } }" );
		final Path oldClient = compile( "client", old, client );
		compile( "recompiled", current, client );
		try ( URLClassLoader loader = new URLClassLoader(
				new java.net.URL[] { oldClient.toUri().toURL(), current.toUri().toURL() }, null ) ) {
			assertEquals( "ok", loader.loadClass( "fixture.Client" ).getMethod( "run" ).invoke( null ) );
		}
		assertTrue( causes( old, current ).isEmpty() );
	}

	@Test
	void constructorsAreNeverResolvedFromSuperclasses() throws Exception {
		final String parent = "package fixture; public class Parent { public Parent(String s) {} }";
		final Path old = compile( "old", null, Map.of( "Parent", parent, "Child",
				"package fixture; public class Child extends Parent { public Child(String s) { super(s); } }" ) );
		final Path current = compile( "current", null, Map.of( "Parent", parent, "Child",
				"package fixture; public class Child extends Parent { public Child() { super(null); } }" ) );
		assertTrue( causes( old, current ).contains( CONSTRUCTOR_REMOVED ) );
	}

	@Test
	void genericReturnChangeBreaksSourceEvenWithIdenticalErasure() throws Exception {
		final Path old = compile( "old", null, Map.of( "Contract",
				"package fixture; public interface Contract { java.util.List<String> values(); }" ) );
		final Path current = compile( "current", null, Map.of( "Contract",
				"package fixture; public interface Contract { java.util.List<Integer> values(); }" ) );
		final Map<String, String> client = Map.of( "Client",
				"package fixture; class Client { String value(Contract c) { return c.values().get(0); } }" );
		compile( "client", old, client );
		assertFalse( compileResult( "recompiled", current, client ) );
		assertEquals( List.of( GENERIC_SIGNATURE_CHANGED ), causes( old, current ) );
	}

	@Test
	void removedCheckedExceptionBreaksProviderRecompilation() throws Exception {
		final Path old = contract( "old", "throws java.io.IOException" );
		final Path current = contract( "current", "" );
		final Map<String, String> provider = Map.of( "Provider",
				"package fixture; public class Provider implements Contract { public void operation() throws java.io.IOException {} }" );
		compile( "provider", old, provider );
		assertFalse( compileResult( "recompiled", current, provider ) );
		assertEquals( List.of( DECLARED_EXCEPTION_REMOVED ), causes( old, current ) );
	}

	@Test
	void narrowedCheckedExceptionBreaksOverridesButWideningDoesNot() throws Exception {
		final Path old = contract( "old", "throws java.io.IOException" );
		final Path narrowed = contract( "narrowed", "throws java.io.FileNotFoundException" );
		final Path widened = contract( "widened", "throws Exception" );
		assertEquals( List.of( DECLARED_EXCEPTION_REMOVED ), causes( old, narrowed ) );
		assertEquals( List.of( DECLARED_EXCEPTION_ADDED ), causes( old, widened ) );
	}

	@Test
	void unavailableExceptionDependencyRequiresReviewInsteadOfAnAssumedBreak() throws Exception {
		final Path dependency = compile( "dependency", null, Map.of( "CustomException",
				"package fixture; public class CustomException extends RuntimeException {}" ) );
		final Path old = compile( "old", dependency, Map.of( "Contract",
				"package fixture; public interface Contract { void operation() throws CustomException; }" ) );
		final Path current = contract( "current", "" );
		final var change = new JavaMigrationCompatibilityAnalyzer().analyze(
				List.of( old.toFile() ), List.of( current.toFile() ) ).getChanges().get( 0 );
		assertEquals( DECLARED_EXCEPTION_REMOVED, change.getCause() );
		assertEquals( JavaMigrationCompatibilityAnalyzer.Certainty.POTENTIAL, change.getCertainty() );
		assertTrue( new JavaMigrationCompatibilityAnalyzer().analyze(
				List.of( old.toFile(), dependency.toFile() ), List.of( current.toFile(), dependency.toFile() ),
				List.of( "type:fixture.Contract" ) ).getChanges().isEmpty() );
	}

	@Test
	void uncheckedExceptionDeclarationsDoNotChangeCompatibility() throws Exception {
		final Path old = contract( "old", "throws IllegalArgumentException, Error" );
		final Path current = contract( "current", "throws IllegalStateException" );
		assertTrue( causes( old, current ).isEmpty() );
	}

	@Test
	void privateAncestorMethodDoesNotReplaceRemovedPublicMethod() throws Exception {
		final String parent = "package fixture; public class Parent { private void value() {} }";
		final Path old = compile( "old", null, Map.of( "Parent", parent, "Child",
				"package fixture; public class Child extends Parent { public void value() {} }" ) );
		final Path current = compile( "current", null, Map.of( "Parent", parent, "Child",
				"package fixture; public class Child extends Parent {}" ) );
		assertEquals( List.of( METHOD_REMOVED ), causes( old, current ) );
	}

	@Test
	void constructorOverloadCanMakeExistingSourceAmbiguous() throws Exception {
		final Path old = compile( "old", null, Map.of( "Choice",
				"package fixture; public class Choice { public Choice(String s) {} }" ) );
		final Path current = compile( "current", null, Map.of( "Choice",
				"package fixture; public class Choice { public Choice(String s) {} public Choice(Integer i) {} }" ) );
		final Map<String, String> client = Map.of( "Client",
				"package fixture; class Client { Object value = new Choice(null); }" );
		compile( "client", old, client );
		assertFalse( compileResult( "recompiled", current, client ) );
		assertEquals( List.of( OVERLOAD_ADDED ), causes( old, current ) );
	}

	private Path contract(String name, String exceptions) throws IOException {
		return compile( name, null, Map.of( "Contract",
				"package fixture; public interface Contract { void operation() " + exceptions + "; }" ) );
	}

	private List<JavaMigrationCompatibilityAnalyzer.Cause> causes(Path old, Path current) {
		return new JavaMigrationCompatibilityAnalyzer().analyze( List.of( old.toFile() ), List.of( current.toFile() ) )
				.getChanges().stream().map( JavaMigrationCompatibilityAnalyzer.Change::getCause ).toList();
	}

	private Path compile(String name, Path classpath, Map<String, String> sources) throws IOException {
		assertTrue( compileResult( name, classpath, sources ) );
		return directory.resolve( name + "/classes" );
	}

	private boolean compileResult(String name, Path classpath, Map<String, String> sources) throws IOException {
		final Path root = directory.resolve( name );
		Files.createDirectories( root.resolve( "classes" ) );
		final List<Path> files = new ArrayList<>();
		for ( Map.Entry<String, String> source : sources.entrySet() ) {
			final Path file = root.resolve( source.getKey() + ".java" );
			Files.writeString( file, source.getValue() );
			files.add( file );
		}
		final var compiler = ToolProvider.getSystemJavaCompiler();
		try ( var manager = compiler.getStandardFileManager( null, null, null ) ) {
			final List<String> options = new ArrayList<>( List.of( "-proc:none", "-d", root.resolve( "classes" ).toString() ) );
			if ( classpath != null ) {
				options.addAll( List.of( "-classpath", classpath.toString() ) );
			}
			return compiler.getTask( new StringWriter(), manager, null, options, null,
					manager.getJavaFileObjectsFromPaths( files ) ).call();
		}
	}
}
