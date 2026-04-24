/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.MetaData;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;

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
	public void testBasic() {
		assertTrue(entities.size() >= 3,
				"There should be at least three entities!");
		ClassDetails basic = findByTableName("BASIC");
		assertNotNull(basic, "BASIC entity should be found");
		assertTrue(basic.getFields().size() >= 2,
				"BASIC should have at least 2 fields");
		// Find the field with @Id annotation
		FieldDetails idField = null;
		for (FieldDetails fd : basic.getFields()) {
			if (fd.getDirectAnnotationUsage(Id.class) != null) {
				idField = fd;
				break;
			}
		}
		assertNotNull(idField, "There should be a field with @Id!");
	}

	@Test
	public void testScalePrecisionLength() {
		ClassDetails basic = findByTableName("BASIC");
		assertNotNull(basic);
		FieldDetails nameField = null;
		for (FieldDetails fd : basic.getFields()) {
			Column col = fd.getDirectAnnotationUsage(Column.class);
			if (col != null && "name".equalsIgnoreCase(
					col.name().replace("`", ""))) {
				nameField = fd;
				break;
			}
		}
		assertNotNull(nameField, "NAME field should be found");
		Column col = nameField.getDirectAnnotationUsage(Column.class);
		assertEquals(20, col.length());
	}

	@Test
	public void testCompositeKeys() {
		ClassDetails multikeyed = findByTableName("MULTIKEYED");
		assertNotNull(multikeyed, "MULTIKEYED entity should be found");
		FieldDetails embeddedIdField = null;
		for (FieldDetails fd : multikeyed.getFields()) {
			if (fd.getDirectAnnotationUsage(EmbeddedId.class) != null) {
				embeddedIdField = fd;
				break;
			}
		}
		assertNotNull(embeddedIdField,
				"MULTIKEYED should have an @EmbeddedId field");
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
