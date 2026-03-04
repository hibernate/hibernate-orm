/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AttributeOverrideMetadata}.
 *
 * @author Koen Aers
 */
public class AttributeOverrideMetadataTest {

	@Test
	public void testConstructorAndGetters() {
		AttributeOverrideMetadata override =
			new AttributeOverrideMetadata("street", "HOME_STREET");

		assertEquals("street", override.getFieldName());
		assertEquals("HOME_STREET", override.getColumnName());
	}
}
