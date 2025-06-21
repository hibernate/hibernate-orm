/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.blob;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class BlobTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-38")
	@WithClasses(BlobEntity.class)
	void testBlobField() {
		assertMetamodelClassGeneratedFor( BlobEntity.class );
		assertPresenceOfFieldInMetamodelFor( BlobEntity.class, "blob", "the metamodel should have a member 'blob'" );
	}
}
