/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddedid.withoutinheritance;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithMappingFiles;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class EmbeddedIdNoInheritanceTest extends CompilationTest {
	@Test
	@WithClasses({ Person.class, XmlPerson.class, PersonId.class })
	@WithMappingFiles("orm.xml")
	public void testGeneratedAnnotationNotGenerated() {
		assertMetamodelClassGeneratedFor( Person.class );
		assertPresenceOfFieldInMetamodelFor(
				Person.class, "id", "Property id should be in metamodel"
		);

		assertPresenceOfFieldInMetamodelFor(
				Person.class, "address", "Property id should be in metamodel"
		);

		assertPresenceOfFieldInMetamodelFor(
				XmlPerson.class, "id", "Property id should be in metamodel"
		);

		assertPresenceOfFieldInMetamodelFor(
				XmlPerson.class, "address", "Property id should be in metamodel"
		);
	}
}
