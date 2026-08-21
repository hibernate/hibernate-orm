/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelperTest {

	@Test
	public void testDetermineClassName() {
		File root = new File("/project/build/classes");
		File classFile = new File("/project/build/classes/com/example/MyClass.class");
		assertEquals("com.example.MyClass", Helper.determineClassName(root, classFile));
	}

	@Test
	public void testDetermineClassNameDefaultPackage() {
		File root = new File("/project/build/classes");
		File classFile = new File("/project/build/classes/MyClass.class");
		assertEquals("MyClass", Helper.determineClassName(root, classFile));
	}

	@Test
	public void testDetermineClassNameDeepPackage() {
		File root = new File("/project/build/classes");
		File classFile = new File("/project/build/classes/com/example/deep/nested/pkg/Service.class");
		assertEquals("com.example.deep.nested.pkg.Service", Helper.determineClassName(root, classFile));
	}
}
