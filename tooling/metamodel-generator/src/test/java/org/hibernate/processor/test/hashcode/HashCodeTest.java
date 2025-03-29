/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hashcode;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAbsenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class HashCodeTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-76")
	@WithClasses(HashEntity.class)
	public void testHashCodeDoesNotCreateSingularAttribute() {
		assertMetamodelClassGeneratedFor( HashEntity.class );

		assertPresenceOfFieldInMetamodelFor( HashEntity.class, "id" );
		assertAbsenceOfFieldInMetamodelFor( HashEntity.class, "hashCode", "hashCode is not a persistent property" );
	}
}
