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
package org.hibernate.tool.reveng.cfg.JDBCMetaDataConfiguration;

import java.util.List;

import jakarta.persistence.Table;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testReadFromJDBC() {
		List<ClassDetails> entities = getEntities(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null));
		assertNotNull(findByEntityName(entities, "WithRealTimestamp"), "WithRealTimestamp");
		assertNotNull(findByEntityName(entities, "NoVersion"), "NoVersion");
		assertNotNull(findByEntityName(entities, "WithFakeTimestamp"), "WithFakeTimestamp");
		assertNotNull(findByEntityName(entities, "WithVersion"), "WithVersion");
	}

	@Test
	public void testGetTable() {
		List<ClassDetails> entities = getEntities(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null));
		assertNotNull(
				findByTableName(entities, JdbcUtil.toIdentifier(this, "WITH_REAL_TIMESTAMP")));
	}

	private List<ClassDetails> getEntities(MetadataDescriptor descriptor) {
		return ((RevengMetadataDescriptor) descriptor).getEntityClassDetails();
	}

	private ClassDetails findByEntityName(List<ClassDetails> entities, String entityName) {
		for (ClassDetails cd : entities) {
			String simpleName = cd.getName();
			int dot = simpleName.lastIndexOf('.');
			if (dot >= 0) {
				simpleName = simpleName.substring(dot + 1);
			}
			if (simpleName.equals(entityName)) {
				return cd;
			}
		}
		return null;
	}

	private ClassDetails findByTableName(List<ClassDetails> entities, String tableName) {
		for (ClassDetails cd : entities) {
			Table tableAnn = cd.getDirectAnnotationUsage(Table.class);
			if (tableAnn != null && tableName.equalsIgnoreCase(
					tableAnn.name().replace("`", ""))) {
				return cd;
			}
		}
		return null;
	}

}
