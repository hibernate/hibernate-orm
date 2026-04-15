/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
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
package org.hibernate.tool.jdbc2cfg.SearchEscapeString;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private List<ClassDetails> entities = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		entities = ((RevengMetadataDescriptor) MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null))
				.getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testBasic() {
		assertTrue(entities.size() >= 2,
				"There should be at least 2 entities!");
		ClassDetails bTab = findByTableName("B_TAB");
		ClassDetails b2Tab = findByTableName("B2TAB");
		assertNotNull(bTab, "B_TAB entity should be found");
		assertNotNull(b2Tab, "B2TAB entity should be found");
		assertTrue(bTab.getFields().size() >= 2,
				"B_TAB should have at least 2 fields");
		assertTrue(b2Tab.getFields().size() >= 2,
				"B2TAB should have at least 2 fields");
	}

	private ClassDetails findByTableName(String tableName) {
		for (ClassDetails cd : entities) {
			jakarta.persistence.Table tableAnn =
					cd.getDirectAnnotationUsage(jakarta.persistence.Table.class);
			if (tableAnn != null && tableName.equalsIgnoreCase(
					tableAnn.name().replace("`", ""))) {
				return cd;
			}
		}
		return null;
	}

}
