/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AttributeOverrideDescriptor}.
 *
 * @author Koen Aers
 */
public class AttributeOverrideDescriptorTest {

	@Test
	public void testConstructorAndGetters() {
		AttributeOverrideDescriptor override =
			new AttributeOverrideDescriptor("street", "HOME_STREET");

		assertEquals("street", override.getFieldName());
		assertEquals("HOME_STREET", override.getColumnName());
	}
}
