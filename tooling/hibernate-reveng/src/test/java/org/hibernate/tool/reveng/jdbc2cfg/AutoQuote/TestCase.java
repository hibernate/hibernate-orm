/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.AutoQuote;

import java.util.List;

import jakarta.persistence.Table;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
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
	public void testForQuotes() {
		ClassDetails usErs = findByTableName("us-ers");
		assertNotNull(usErs, "Entity mapped to 'us-ers' table should exist");
		Table tableAnn = usErs.getDirectAnnotationUsage(Table.class);
		assertNotNull(tableAnn);
		assertTrue(tableAnn.name().contains("us-ers"),
				"Table name should contain 'us-ers'");
		assertTrue(usErs.getFields().size() >= 2,
				"'us-ers' entity should have at least 2 fields");

		ClassDetails worklogs = findByTableName("WORKLOGS");
		assertNotNull(worklogs, "Entity mapped to 'WORKLOGS' table should exist");
		FieldDetails usErsField = findField(worklogs, "usErs");
		assertNotNull(usErsField, "WORKLOGS should have a 'usErs' field");
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

	private FieldDetails findField(ClassDetails classDetails, String fieldName) {
		for (FieldDetails field : classDetails.getFields()) {
			if (fieldName.equals(field.getName())) {
				return field;
			}
		}
		return null;
	}

}
