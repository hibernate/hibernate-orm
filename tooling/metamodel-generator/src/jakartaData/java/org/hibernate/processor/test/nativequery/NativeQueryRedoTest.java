package org.hibernate.processor.test.nativequery;

import jakarta.persistence.TypedQueryReference;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getMethodFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CompilationTest
class NativeQueryRedoTest {

	@Test
	@TestForIssue(jiraKey = "HHH-20969")
	@WithClasses(value = RedoBook.class, sources = "org.hibernate.processor.test.nativequery.DeferredNativeQueries")
	void nativeQueryWithSessionGeneratesCompanionOnRedo() {
		final var queryReference = getMethodFromMetamodelFor(
				"org.hibernate.processor.test.nativequery.DeferredNativeQueries", "findByTitle", String.class );
		assertEquals( TypedQueryReference.class, queryReference.getReturnType() );
	}
}
