/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SimpleLexerExceptionTest {

	@Test
	public void testDefaultConstructor() {
		SimpleLexerException ex = new SimpleLexerException();
		assertNull(ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	public void testMessageConstructor() {
		SimpleLexerException ex = new SimpleLexerException("syntax error");
		assertEquals("syntax error", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	public void testCauseConstructor() {
		RuntimeException cause = new RuntimeException("root cause");
		SimpleLexerException ex = new SimpleLexerException(cause);
		assertSame(cause, ex.getCause());
	}

	@Test
	public void testMessageAndCauseConstructor() {
		RuntimeException cause = new RuntimeException("root cause");
		SimpleLexerException ex = new SimpleLexerException("syntax error", cause);
		assertEquals("syntax error", ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	public void testIsRuntimeException() {
		SimpleLexerException ex = new SimpleLexerException("test");
		assertInstanceOf(RuntimeException.class, ex);
	}
}
