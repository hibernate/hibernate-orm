/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.ABSTRACT_METHOD_ADDED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.CONSTANT_VALUE_CHANGED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.CONSTRUCTOR_VISIBILITY_REDUCED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.DEFAULT_METHOD_ADDED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.FIELD_TYPE_CHANGED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.METHOD_BECAME_FINAL;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.METHOD_RETURN_TYPE_CHANGED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.TYPE_BECAME_FINAL;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Impact.BEHAVIORAL;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Impact.SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Focused normalized Java migration-change tests.
///
/// @author Steve Ebersole
public class JavaMigrationCompatibilityAnalyzerTests {
	@TempDir
	Path temporaryDirectory;

	@Test
	public void detectsInterfaceAndCallableSurfaceChanges() throws IOException {
		final Path baseline = temporaryDirectory.resolve( "baseline" );
		final Path current = temporaryDirectory.resolve( "current" );
		writeClass(
				baseline,
				"fixture/Contract",
				Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
				writer -> {
					writer.visitField(
							Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
							"VALUE",
							"I",
							null,
							1
					).visitEnd();
					writer.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "value", "()Ljava/lang/String;", null, null ).visitEnd();
				}
		);
		writeClass(
				current,
				"fixture/Contract",
				Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
				writer -> {
					writer.visitField(
							Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
							"VALUE",
							"J",
							null,
							2L
					).visitEnd();
					writer.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "value", "()Ljava/lang/Object;", null, null ).visitEnd();
					writer.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "required", "()V", null, null ).visitEnd();
					writer.visitMethod( Opcodes.ACC_PUBLIC, "optional", "()V", null, null ).visitEnd();
				}
		);

		final List<JavaMigrationCompatibilityAnalyzer.Change> changes = analyze( baseline, current );
		assertCause( changes, FIELD_TYPE_CHANGED, "field:fixture.Contract#VALUE" );
		assertCause( changes, CONSTANT_VALUE_CHANGED, "field:fixture.Contract#VALUE" );
		assertCause( changes, METHOD_RETURN_TYPE_CHANGED, "method:fixture.Contract#value()" );
		assertCause( changes, ABSTRACT_METHOD_ADDED, "method:fixture.Contract#required()" );
		assertCause( changes, DEFAULT_METHOD_ADDED, "method:fixture.Contract#optional()" );
		assertEquals(
				EnumSet.of( SOURCE, BEHAVIORAL ),
				change( changes, CONSTANT_VALUE_CHANGED, "field:fixture.Contract#VALUE" ).getImpacts()
		);
	}

	@Test
	public void detectsSubclassSurfaceChanges() throws IOException {
		final Path baseline = temporaryDirectory.resolve( "baseline" );
		final Path current = temporaryDirectory.resolve( "current" );
		writeClass(
				baseline,
				"fixture/Base",
				Opcodes.ACC_PUBLIC,
				writer -> {
					writer.visitMethod( Opcodes.ACC_PROTECTED, "<init>", "()V", null, null ).visitEnd();
					writer.visitMethod( Opcodes.ACC_PROTECTED, "hook", "()V", null, null ).visitEnd();
				}
		);
		writeClass(
				current,
				"fixture/Base",
				Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
				writer -> {
					writer.visitMethod( Opcodes.ACC_PRIVATE, "<init>", "()V", null, null ).visitEnd();
					writer.visitMethod( Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL, "hook", "()V", null, null ).visitEnd();
				}
		);

		final List<JavaMigrationCompatibilityAnalyzer.Change> changes = analyze( baseline, current );
		assertCause( changes, TYPE_BECAME_FINAL, "type:fixture.Base" );
		assertCause( changes, CONSTRUCTOR_VISIBILITY_REDUCED, "constructor:fixture.Base#<init>()" );
		assertCause( changes, METHOD_BECAME_FINAL, "method:fixture.Base#hook()" );
	}

	private List<JavaMigrationCompatibilityAnalyzer.Change> analyze(Path baseline, Path current) {
		return new JavaMigrationCompatibilityAnalyzer()
				.analyze( List.of( baseline.toFile() ), List.of( current.toFile() ) )
				.getChanges();
	}

	private static void assertCause(
			List<JavaMigrationCompatibilityAnalyzer.Change> changes,
			JavaMigrationCompatibilityAnalyzer.Cause cause,
			String elementId) {
		assertTrue(
				changes.stream().anyMatch( change -> change.getCause() == cause && change.getElementId().equals( elementId ) ),
				() -> "Missing " + cause + " for " + elementId
		);
	}

	private static JavaMigrationCompatibilityAnalyzer.Change change(
			List<JavaMigrationCompatibilityAnalyzer.Change> changes,
			JavaMigrationCompatibilityAnalyzer.Cause cause,
			String elementId) {
		return changes.stream()
				.filter( change -> change.getCause() == cause && change.getElementId().equals( elementId ) )
				.findFirst()
				.orElseThrow();
	}

	private static void writeClass(
			Path root,
			String internalName,
			int access,
			Consumer<ClassWriter> declarations) throws IOException {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, access, internalName, null, "java/lang/Object", null );
		declarations.accept( writer );
		writer.visitEnd();
		final Path classFile = root.resolve( internalName + ".class" );
		Files.createDirectories( classFile.getParent() );
		Files.write( classFile, writer.toByteArray() );
	}
}
