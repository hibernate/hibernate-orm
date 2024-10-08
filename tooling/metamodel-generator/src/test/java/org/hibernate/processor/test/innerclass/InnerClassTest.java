/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.innerclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

public class InnerClassTest extends CompilationTest {

	@WithClasses({Person.class, Dummy.class})
	@Test
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Person.class ) );
	}
}
