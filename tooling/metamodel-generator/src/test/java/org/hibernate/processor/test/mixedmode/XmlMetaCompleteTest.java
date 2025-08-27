/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mixedmode;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAbsenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class XmlMetaCompleteTest {
	@Test
	@WithClasses(Person.class)
	@WithMappingFiles("orm.xml")
	void testXmlConfiguredEmbeddedClassGenerated() {
		assertMetamodelClassGeneratedFor( Person.class );
		assertAbsenceOfFieldInMetamodelFor( Person.class, "name" );
	}
}
