package org.hibernate.processor.test.data.entityprojection;

import java.nio.file.Files;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getOutBaseDir;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class EntityRecordProjectionTest {

	@Test
	@WithClasses(value = {}, sources = {
			"org.hibernate.processor.test.data.entityprojection.Book",
			"org.hibernate.processor.test.data.entityprojection.Author",
			"org.hibernate.processor.test.data.entityprojection.Library"
	})
	void recordProjectionOfEntitiesBeingCompiled() {
		// Keep the entities in resources so they are not loadable by the processor.
		// Compilation must succeed even when the selections' Java classes are unknown.
		assertTrue( Files.exists( getOutBaseDir( getClass() ).toPath().resolve(
				"org/hibernate/processor/test/data/entityprojection/Library_.class" ) ) );
	}
}
