/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExceptionUtilTest {

	@Test
	public void testNull() {
		assertNull(ExceptionUtil.getProblemSolutionOrCause(null));
	}

	@Test
	public void testClassNotFoundException() {
		String result = ExceptionUtil.getProblemSolutionOrCause(new ClassNotFoundException("some.Class"));
		assertNotNull(result);
		assertTrue(result.contains("classpath"));
	}

	@Test
	public void testNoClassDefFoundError() {
		String result = ExceptionUtil.getProblemSolutionOrCause(new NoClassDefFoundError("some/Class"));
		assertNotNull(result);
		assertTrue(result.contains("classpath"));
	}

	@Test
	public void testUnsupportedClassVersionError() {
		String result = ExceptionUtil.getProblemSolutionOrCause(new UnsupportedClassVersionError("bad version"));
		assertNotNull(result);
		assertTrue(result.contains("JRE"));
	}

	@Test
	public void testGenericExceptionWithNoCause() {
		RuntimeException ex = new RuntimeException("generic");
		assertNull(ExceptionUtil.getProblemSolutionOrCause(ex));
	}

	@Test
	public void testNestedClassNotFoundException() {
		RuntimeException wrapper = new RuntimeException("wrapper", new ClassNotFoundException("nested"));
		String result = ExceptionUtil.getProblemSolutionOrCause(wrapper);
		assertNotNull(result);
		assertTrue(result.contains("classpath"));
	}

	@Test
	public void testSelfReferencingCause() {
		// Simulate a cause that references itself (cause == this)
		RuntimeException ex = new RuntimeException("self") {
			@Override
			public synchronized Throwable getCause() {
				return this;
			}
		};
		assertNull(ExceptionUtil.getProblemSolutionOrCause(ex));
	}
}
