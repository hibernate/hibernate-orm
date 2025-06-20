/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlmapped;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class IgnoreInvalidXmlTest {
	@Test
	@WithClasses(Superhero.class)
	@WithMappingFiles({ "orm.xml", "jpa1-orm.xml", "malformed-mapping.xml", "non-existend-class.xml" })
	void testInvalidXmlFilesGetIgnored() {
		// this is only a indirect test, but if the invalid xml files would cause the processor to abort the
		// meta class would not have been generated
		assertMetamodelClassGeneratedFor( Superhero.class );
	}
}
