/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.sortedcollection;

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
class SortedCollectionTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-62")
	@WithClasses({ Printer.class, PrintJob.class })
	void testGenerics() {
		assertMetamodelClassGeneratedFor( Printer.class );
		assertMetamodelClassGeneratedFor( PrintJob.class );
		assertPresenceOfFieldInMetamodelFor( Printer.class, "printQueue", "There sorted set attribute is missing" );
		assertPresenceOfFieldInMetamodelFor( Printer.class, "printedJobs", "There sorted map attribute is missing" );
	}
}
