/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddedid.withoutinheritance;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

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
