/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.circulartypevariable;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;


@TestForIssue(jiraKey = "HHH-17253")
public class CircularTypeVariableTest extends CompilationTest {

	@Test
	@WithClasses({ RoleAccess.class, User.class })
	public void testCircularTypeVariable() {
		TestUtil.assertMetamodelClassGeneratedFor( RoleAccess.class );
		TestUtil.assertMetamodelClassGeneratedFor( User.class );
	}

}
