/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2022-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.orm.jbt.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.junit.jupiter.api.Test;

public class DummyMetadataDescriptorTest {
	
	@Test
	public void testConstruction() {
		assertTrue(new DummyMetadataDescriptor() instanceof MetadataDescriptor);
	}
	
	@Test
	public void testGetProperties() {
		assertNull(new DummyMetadataDescriptor().getProperties());
	}
	
	@Test
	public void testCreateMetadata() {
		Metadata metadata = new DummyMetadataDescriptor().createMetadata();
		assertNotNull(metadata);
		assertEquals(Collections.emptySet(), metadata.getEntityBindings());
		assertEquals(Collections.emptySet(), metadata.collectTableMappings());
	}

}
