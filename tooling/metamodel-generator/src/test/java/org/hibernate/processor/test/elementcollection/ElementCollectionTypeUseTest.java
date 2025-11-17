/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.elementcollection;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertListAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMapAttributesInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertSetAttributeTypeInMetaModelFor;

/**
 * @author Chris Cranford
 */
@CompilationTest
class ElementCollectionTypeUseTest {
	@Test
	@TestForIssue(jiraKey = "HHH-12612")
	@WithClasses(OfficeBuildingValidated.class)
	void testAnnotatedCollectionElements() {
		System.out.println( TestUtil.getMetaModelSourceAsString( OfficeBuildingValidated.class ) );
		assertMetamodelClassGeneratedFor( OfficeBuildingValidated.class );

		assertMapAttributesInMetaModelFor(
				OfficeBuildingValidated.class,
				"doorCodes",
				Integer.class,
				byte[].class,
				"Wrong type in map attributes."
		);

		assertSetAttributeTypeInMetaModelFor(
				OfficeBuildingValidated.class,
				"computerSerialNumbers",
				String.class,
				"Wrong type in set attribute."
		);

		assertListAttributeTypeInMetaModelFor(
				OfficeBuildingValidated.class,
				"employeeNames",
				String.class,
				"Wrong type in list attributes."
		);

		assertListAttributeTypeInMetaModelFor(
				OfficeBuildingValidated.class,
				"rooms",
				Room.class,
				"Wrong type in list attributes."
		);
	}
}
