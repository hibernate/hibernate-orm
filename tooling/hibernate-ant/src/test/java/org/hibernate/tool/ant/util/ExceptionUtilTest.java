/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.util;

import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
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
	public void testMappingNotFoundException() {
		Origin origin = new Origin(SourceType.RESOURCE, "com/example/Foo.hbm.xml");
		MappingNotFoundException ex = new MappingNotFoundException(origin);
		String result = ExceptionUtil.getProblemSolutionOrCause(ex);
		assertNotNull(result);
		assertTrue(result.contains("com/example/Foo.hbm.xml"));
		assertTrue(result.contains("RESOURCE"));
	}

	@Test
	public void testClassNotFoundException() {
		String result = ExceptionUtil.getProblemSolutionOrCause(
				new ClassNotFoundException("com.example.Missing"));
		assertNotNull(result);
		assertTrue(result.contains("classpath"));
	}

	@Test
	public void testNoClassDefFoundError() {
		String result = ExceptionUtil.getProblemSolutionOrCause(
				new NoClassDefFoundError("com/example/Missing"));
		assertNotNull(result);
		assertTrue(result.contains("classpath"));
	}

	@Test
	public void testUnsupportedClassVersionError() {
		String result = ExceptionUtil.getProblemSolutionOrCause(
				new UnsupportedClassVersionError("bad version"));
		assertNotNull(result);
		assertTrue(result.contains("JRE"));
	}

	@Test
	public void testNestedCause() {
		Exception inner = new ClassNotFoundException("nested");
		RuntimeException outer = new RuntimeException("wrapper", inner);
		String result = ExceptionUtil.getProblemSolutionOrCause(outer);
		assertNotNull(result);
		assertTrue(result.contains("classpath"));
	}

	@Test
	public void testUnknownExceptionNoCause() {
		RuntimeException ex = new RuntimeException("unknown");
		assertNull(ExceptionUtil.getProblemSolutionOrCause(ex));
	}
}
