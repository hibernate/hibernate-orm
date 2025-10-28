/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh17613;

import org.hibernate.processor.test.hhh17613.a.ChildA;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.jupiter.api.Test;

@CompilationTest
@TestForIssue(jiraKey = " HHH-17613")
class HHH17613Test {

	@Test
	@WithClasses({ ChildA.class, ChildB.class, Parent.class })
	@TestForIssue(jiraKey = " HHH-17613")
	void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( ChildA.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( ChildB.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Parent.class ) );
	}
}
