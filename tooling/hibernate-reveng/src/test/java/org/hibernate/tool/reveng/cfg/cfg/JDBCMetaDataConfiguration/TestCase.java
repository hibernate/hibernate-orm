/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
