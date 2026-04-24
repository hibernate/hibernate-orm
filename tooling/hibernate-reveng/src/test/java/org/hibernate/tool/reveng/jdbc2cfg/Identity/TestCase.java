/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.Identity;

import java.util.List;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
	public void testIdentity() {
		ClassDetails autoinc = findByTableName("AUTOINC");
		assertNotNull(autoinc);
		FieldDetails idField = findIdField(autoinc);
		assertNotNull(idField, "AUTOINC should have an @Id field");
		GeneratedValue genVal = idField.getDirectAnnotationUsage(GeneratedValue.class);
		assertNotNull(genVal, "AUTOINC @Id field should have @GeneratedValue");
		assertEquals(GenerationType.IDENTITY, genVal.strategy());

		ClassDetails noautoinc = findByTableName("NOAUTOINC");
		assertNotNull(noautoinc);
		FieldDetails noAutoIdField = findIdField(noautoinc);
		assertNotNull(noAutoIdField, "NOAUTOINC should have an @Id field");
		GeneratedValue noAutoGenVal = noAutoIdField.getDirectAnnotationUsage(GeneratedValue.class);
		assertNull(noAutoGenVal, "NOAUTOINC @Id field should not have @GeneratedValue");
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

	private FieldDetails findIdField(ClassDetails classDetails) {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.getDirectAnnotationUsage(Id.class) != null) {
				return field;
			}
		}
		return null;
	}

}
