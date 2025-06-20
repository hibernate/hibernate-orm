/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.targetannotation;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class TargetAnnotationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-30")
	@WithClasses({ Address.class, AddressImpl.class, House.class })
	void testEmbeddableWithTargetAnnotation() {
		assertMetamodelClassGeneratedFor( House.class );
		assertPresenceOfFieldInMetamodelFor( House.class, "address", "the metamodel should have a member 'address'" );
		assertAttributeTypeInMetaModelFor(
				House.class,
				"address",
				AddressImpl.class,
				"The target annotation set the type to AddressImpl"
		);
	}
}
