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
package org.hibernate.tool.jdbc2cfg.Index;

import java.util.List;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
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
	public void testUniqueKey() {
		ClassDetails withIndex = findByTableName("WITH_INDEX");
		assertNotNull(withIndex);
		Table tableAnn = withIndex.getDirectAnnotationUsage(Table.class);
		assertNotNull(tableAnn);
		UniqueConstraint[] uniqueConstraints = tableAnn.uniqueConstraints();
		assertNotNull(uniqueConstraints);
		UniqueConstraint otherIdx = null;
		for (UniqueConstraint uc : uniqueConstraints) {
			if (uc.name() != null && uc.name().replace("`", "")
					.equalsIgnoreCase("OTHER_IDX")) {
				otherIdx = uc;
				break;
			}
		}
		assertNotNull(otherIdx, "UniqueConstraint OTHER_IDX should exist");
		assertEquals(1, otherIdx.columnNames().length);
	}

	@Test
	public void testWithIndex() {
		ClassDetails withIndex = findByTableName("WITH_INDEX");
		assertNotNull(withIndex);
		// The table has no primary key, so there should be no @Id field
		boolean hasId = false;
		for (FieldDetails field : withIndex.getFields()) {
			if (field.getDirectAnnotationUsage(Id.class) != null) {
				hasId = true;
				break;
			}
		}
		assertFalse(hasId, "there should be no pk");
		// Verify the entity has the expected fields
		assertTrue(withIndex.getFields().size() >= 3,
				"WITH_INDEX should have at least 3 fields (one, two, three)");
	}

	private ClassDetails findByTableName(String tableName) {
		for (ClassDetails cd : entities) {
			Table tableAnn = cd.getDirectAnnotationUsage(Table.class);
			if (tableAnn != null) {
				String name = tableAnn.name().replace("`", "");
				if (tableName.equals(name) || tableName.equalsIgnoreCase(name)) {
					return cd;
				}
			}
		}
		return null;
	}

}
