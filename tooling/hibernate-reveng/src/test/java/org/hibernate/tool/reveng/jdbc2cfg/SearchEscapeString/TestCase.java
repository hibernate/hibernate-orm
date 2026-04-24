/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.SearchEscapeString;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
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
