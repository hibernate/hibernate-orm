/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ImportScriptExceptionTest {

	@Test
	public void testMessageConstructor() {
		ImportScriptException ex = new ImportScriptException("import failed");
		assertEquals("import failed", ex.getMessage());
	}

	@Test
	public void testMessageAndCauseConstructor() {
		RuntimeException cause = new RuntimeException("root cause");
		ImportScriptException ex = new ImportScriptException("import failed", cause);
		assertEquals("import failed", ex.getMessage());
		assertNotNull(ex.getCause());
		assertEquals("root cause", ex.getCause().getMessage());
	}
}
