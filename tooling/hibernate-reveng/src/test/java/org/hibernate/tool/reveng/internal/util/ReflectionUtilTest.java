/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ReflectionUtilTest {

	@Test
	public void testClassForName() throws Exception {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		assertNotNull(contextClassLoader);
		try {
			Class<?> clazz = ReflectionUtil.classForName("org.hibernate.tool.reveng.internal.util.ReflectionUtil");
			assertSame(ReflectionUtil.class, clazz);
			Thread.currentThread().setContextClassLoader(null);
			clazz = ReflectionUtil.classForName("org.hibernate.tool.reveng.internal.util.ReflectionUtil");
			assertSame(ReflectionUtil.class, clazz);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

}
