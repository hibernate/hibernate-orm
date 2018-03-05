/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.elementcollection;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithMappingFiles;

import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertListAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMapAttributesInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertNoSourceFileGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
public class ElementCollectionTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-8")
	@WithClasses({ House.class, Room.class })
	public void testElementCollectionOnMap() {
		assertMetamodelClassGeneratedFor( House.class );
		assertMetamodelClassGeneratedFor( Room.class );
		// side effect of METAGEN-8 was that a meta class for String was created!
		assertNoSourceFileGeneratedFor( String.class );
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-19")
	@WithClasses({ Hotel.class, Room.class, Cleaner.class })
	public void testMapKeyClass() {
		assertMetamodelClassGeneratedFor( Hotel.class );
		assertMapAttributesInMetaModelFor(
				Hotel.class, "roomsByName", String.class, Room.class, "Wrong type in map attribute."
		);

		assertMapAttributesInMetaModelFor(
				Hotel.class, "cleaners", Room.class, Cleaner.class, "Wrong type in map attribute."
		);
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-22")
	@WithClasses({ Hostel.class, Room.class, Cleaner.class })
	@WithMappingFiles("hostel.xml")
	public void testMapKeyClassXmlConfigured() {
		assertMetamodelClassGeneratedFor( Hostel.class );
		assertMapAttributesInMetaModelFor(
				Hostel.class, "roomsByName", String.class, Room.class, "Wrong type in map attribute."
		);

		assertMapAttributesInMetaModelFor(
				Hostel.class, "cleaners", Room.class, Cleaner.class, "Wrong type in map attribute."
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11004")
	@WithClasses({ OfficeBuilding.class })
	public void testArrayValueElementCollection() {
		assertMetamodelClassGeneratedFor( OfficeBuilding.class );
		assertMapAttributesInMetaModelFor(
				OfficeBuilding.class, "doorCodes", Integer.class, byte[].class, "Wrong type in map attribute."
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11871")
	@WithClasses({ Homework.class})
	public void testListAttributeWithGenericTypeForJavaBeanGetter() {
		assertMetamodelClassGeneratedFor( Homework.class );
		assertListAttributeTypeInMetaModelFor( Homework.class, "paths", String.class, "ListAttribute generic type should be String" );
	}
}
