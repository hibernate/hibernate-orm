/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a user provides their own {@code @StaticMetamodel}-annotated class,
 * the processor must not overwrite it — even though it carries the same
 * annotation that a generated metamodel would have.
 */
@CompilationTest
@TestForIssue(jiraKey = "HHH-20775")
class HandwrittenMetamodelTest {

	@Test
	void testHandwrittenStaticMetamodelSuppressesGeneration() throws Exception {
		final var outDir = new File( TestUtil.getOutBaseDir( HandwrittenMetamodelTest.class ), "handwritten" );
		TestUtil.deleteFilesRecursive( outDir );
		final var classesDir = new File( outDir, "classes" );
		final var generatedSourcesDir = new File( outDir, "generated-sources" );
		Files.createDirectories( classesDir.toPath() );
		Files.createDirectories( generatedSourcesDir.toPath() );

		final var entitySource = TestUtil.getSourceFile( HandwrittenEntity.class );
		final var handwrittenMetamodelSource = TestUtil.getSourceFile( _HandwrittenEntity.class );
		final var unwantedGeneratedMetamodelSource = TestUtil.getSourceFile( _HandwrittenEntity.class, generatedSourcesDir );
		final var handwrittenMetamodelClass = TestUtil.getClassFile( _HandwrittenEntity.class, classesDir );

		// Compile the handwritten input from src/jakartaData/java into classesDir, without generating sources.
		TestUtil.compileWithoutProcessor( classesDir, entitySource, handwrittenMetamodelSource );
		assertTrue( handwrittenMetamodelClass.exists(), "Handwritten metamodel must be available on the classpath" );

		// Process only the entity, with the compiled handwritten metamodel on the classpath.
		TestUtil.compile( classesDir, generatedSourcesDir, new File[] { entitySource } );

		assertTrue( handwrittenMetamodelSource.exists(), "The handwritten source remains in the input source directory" );
		assertFalse( unwantedGeneratedMetamodelSource.exists(),
				"Processor must not create a second _HandwrittenEntity.java in the generated-source directory" );
	}
}
