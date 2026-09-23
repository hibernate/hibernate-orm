/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
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
	private final File outDir = new File( TestUtil.getOutBaseDir( HandwrittenMetamodelTest.class ), "handwritten" );
	private final File classesDir = new File( outDir, "classes" );
	private final File generatedSourcesDir = new File( outDir, "generated-sources" );
	private final File entitySource = TestUtil.getSourceFile( HandwrittenEntity.class );
	private final File handwrittenMetamodelSource = TestUtil.getSourceFile( _HandwrittenEntity.class );
	private final File unwantedGeneratedMetamodelSource = TestUtil.getSourceFile( _HandwrittenEntity.class, generatedSourcesDir );
	private final File handwrittenMetamodelClass = TestUtil.getClassFile( _HandwrittenEntity.class, classesDir );

	@BeforeEach
	void prepareOutputDirectories() throws Exception {
		TestUtil.deleteFilesRecursive( outDir );
		Files.createDirectories( classesDir.toPath() );
		Files.createDirectories( generatedSourcesDir.toPath() );
	}

	@Test
	void testHandwrittenStaticMetamodelFromClasspath() throws Exception {
		// The compiled _HandwrittenEntity fixture is on the test classpath, like the TCK JAR.
		// Compile the entity and repository without supplying the metamodel source or SOURCE_PATH.
		// Regenerating the metamodel would shadow the dependency and lose HANDWRITTEN_NAME.
		TestUtil.compile( classesDir, generatedSourcesDir,
				new File[] { entitySource, TestUtil.getSourceFile( HandwrittenMetamodelRepository.class ) } );

		assertFalse( unwantedGeneratedMetamodelSource.exists(),
				"Processor must preserve the metamodel supplied by a compiled dependency" );
		assertFalse( handwrittenMetamodelClass.exists(),
				"The metamodel must be used from the dependency, not recreated in class output" );
	}

	@Test
	void testHandwrittenStaticMetamodelWithoutExplicitSourcePath() throws Exception {
		// Gradle's TCK compilation passes both files as inputs without setting SOURCE_PATH.
		TestUtil.compile( classesDir, generatedSourcesDir,
				new File[] { entitySource, handwrittenMetamodelSource } );

		assertTrue( handwrittenMetamodelClass.exists() );
		assertFalse( unwantedGeneratedMetamodelSource.exists(),
				"Processor must not generate a metamodel for handwritten compilation input" );
	}

	@Test
	void testHandwrittenStaticMetamodelSuppressesGeneration() throws Exception {
		// Compile the handwritten input from src/jakartaData/java into classesDir, without generating sources.
		TestUtil.compileWithoutProcessor( classesDir, entitySource, handwrittenMetamodelSource );
		assertTrue( handwrittenMetamodelClass.exists(), "Handwritten metamodel must be available on the classpath" );

		// Process only the entity, with the compiled handwritten metamodel on the classpath.
		TestUtil.compile( classesDir, generatedSourcesDir, TestUtil.getSourceBaseDir( HandwrittenEntity.class ),
				new File[] { entitySource } );

		assertTrue( handwrittenMetamodelSource.exists(), "The handwritten source remains in the input source directory" );
		assertFalse( unwantedGeneratedMetamodelSource.exists(),
				"Processor must not create a second _HandwrittenEntity.java in the generated-source directory" );
	}
}
