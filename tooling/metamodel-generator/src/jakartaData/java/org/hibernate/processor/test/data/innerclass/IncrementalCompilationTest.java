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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When an entity is compiled twice (incremental build), the processor must
 * still generate the Jakarta Data static metamodel ({@code _Entity}).
 * <p>
 * Previously, {@code hasHandwrittenMetamodel} would see the {@code _Entity.class}
 * left over from the first compilation and incorrectly skip generating the
 * data metamodel on subsequent compilations.
 */
@CompilationTest
@TestForIssue(jiraKey = "HHH-20775")
class IncrementalCompilationTest {

	@Test
	void testJakartaDataMetamodelRegeneratedOnIncrementalBuild() throws Exception {
		final var outDir = new File( TestUtil.getOutBaseDir( IncrementalCompilationTest.class ), "incremental" );
		outDir.mkdirs();

		final var sourceFile = TestUtil.getSourceFile( IncrementalItemEntity.class );
		final var dataMetamodelSource = new File( outDir,
				"org/hibernate/processor/test/data/innerclass/_IncrementalItemEntity.java" );

		// First compilation: generates _IncrementalItemEntity.java
		TestUtil.compile( outDir, sourceFile );
		assertTrue( dataMetamodelSource.exists(),
				"First compilation should generate _IncrementalItemEntity.java" );

		// Delete the generated source to detect whether the second compilation regenerates it
		assertTrue( dataMetamodelSource.delete() );

		// Second compilation: same source, output dir on classpath (simulates incremental build).
		// The _IncrementalItemEntity.class from the first compilation is visible as a package sibling.
		TestUtil.compile( outDir, sourceFile );
		assertTrue( dataMetamodelSource.exists(),
				"Second compilation should regenerate _IncrementalItemEntity.java" );
	}
}
