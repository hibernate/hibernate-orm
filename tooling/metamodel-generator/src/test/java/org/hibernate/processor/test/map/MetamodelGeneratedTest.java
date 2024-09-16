/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.map;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;

public class MetamodelGeneratedTest extends CompilationTest {

	@Test
	@WithClasses({ MapOfMapEntity.class })
	@TestForIssue(jiraKey = " HHH-17514")
	public void test() {
		Class<?> repositoryClass = getMetamodelClassFor( MapOfMapEntity.class );
		Assertions.assertNotNull( repositoryClass );
	}
}
