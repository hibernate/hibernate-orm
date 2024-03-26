/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.elementcollection;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.*;

/**
 * @author Chris Cranford
 */
public class ElementCollectionTypeUseTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "HHH-12612")
	@WithClasses(OfficeBuildingValidated.class)
	public void testAnnotatedCollectionElements() {
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
