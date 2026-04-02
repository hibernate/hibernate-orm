/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PrimaryKeyInfoTest {

	@Test
	public void testDefaultValues() {
		PrimaryKeyInfo info = new PrimaryKeyInfo();
		assertNull(info.suggestedStrategy);
		assertNull(info.suggestedProperties);
	}

	@Test
	public void testSetValues() {
		PrimaryKeyInfo info = new PrimaryKeyInfo();
		info.suggestedStrategy = "sequence";
		Properties props = new Properties();
		props.setProperty("sequence_name", "my_seq");
		info.suggestedProperties = props;

		assertEquals("sequence", info.suggestedStrategy);
		assertEquals("my_seq", info.suggestedProperties.getProperty("sequence_name"));
	}
}
