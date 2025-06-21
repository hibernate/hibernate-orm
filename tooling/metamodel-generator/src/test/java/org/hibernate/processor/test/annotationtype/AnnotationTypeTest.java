/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.annotationtype;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.jupiter.api.Test;

/**
 * @author Sergey Morgunov
 */
@CompilationTest
@TestForIssue(jiraKey = "HHH-13145")
class AnnotationTypeTest {

	@Test
	@WithClasses({ Entity.class })
	@WithMappingFiles("orm.xml")
	void testXmlConfiguredEntityGenerated() {
		TestUtil.assertMetamodelClassGeneratedFor( Entity.class );
	}

}
