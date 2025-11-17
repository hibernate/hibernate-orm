/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.map;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;

@CompilationTest
class MetamodelGeneratedTest {

	@Test
	@WithClasses({ MapOfMapEntity.class })
	@TestForIssue(jiraKey = " HHH-17514")
	void test() {
		Class<?> repositoryClass = getMetamodelClassFor( MapOfMapEntity.class );
		Assertions.assertNotNull( repositoryClass );
	}
}
