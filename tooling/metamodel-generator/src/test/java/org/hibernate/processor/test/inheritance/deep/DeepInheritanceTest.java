/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.deep;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * Tests a deep class hierarchy mixed with inheritance and a root class that
 * does not declare an id
 *
 * @author Igor Vaynberg
 */
@CompilationTest
class DeepInheritanceTest {
	@Test
	@TestForIssue(jiraKey = "METAGEN-69")
	@WithClasses({ JetPlane.class, PersistenceBase.class, Plane.class })
	void testDeepInheritance() throws Exception {
		assertMetamodelClassGeneratedFor( Plane.class );
		assertMetamodelClassGeneratedFor( JetPlane.class );
		assertPresenceOfFieldInMetamodelFor( JetPlane.class, "jets" );
		assertAttributeTypeInMetaModelFor(
				JetPlane.class,
				"jets",
				Integer.class,
				"jets should be defined in JetPlane_"
		);
	}
}
