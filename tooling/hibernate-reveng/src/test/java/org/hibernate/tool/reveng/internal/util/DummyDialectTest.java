/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DummyDialectTest {

	@Test
	public void testDummyDialect() {
		assertNotNull(DummyDialect.INSTANCE);
	}

}
