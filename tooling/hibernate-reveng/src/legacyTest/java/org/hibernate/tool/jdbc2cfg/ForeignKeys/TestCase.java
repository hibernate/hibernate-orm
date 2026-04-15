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
package org.hibernate.tool.jdbc2cfg.ForeignKeys;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private MetadataDescriptor metadataDescriptor = null;
	private List<ClassDetails> entities = null;
	private RevengStrategy reverseEngineeringStrategy = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		reverseEngineeringStrategy = new DefaultStrategy();
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, null);
		entities = ((RevengMetadataDescriptor) metadataDescriptor).getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testMultiRefs() {
		ClassDetails connection = findByTableName("CONNECTION");
		assertNotNull(connection);
		// CONNECTION has 3 FKs: CON2MASTER -> MASTER, CHILDREF1 -> CHILD, CHILDREF2 -> CHILD
		List<FieldDetails> manyToOneFields = connection.getFields().stream()
				.filter(f -> f.getDirectAnnotationUsage(ManyToOne.class) != null)
				.toList();
		assertEquals(3, manyToOneFields.size());
		// Check that at least one ManyToOne references Master
		boolean hasMasterRef = manyToOneFields.stream()
				.anyMatch(f -> f.getType().determineRawClass().getName().endsWith("Master"));
		assertTrue(hasMasterRef, "CONNECTION should have a ManyToOne referencing Master");
	}

	@Test
	public void testMasterChild() {
		ClassDetails master = findByTableName("MASTER");
		assertNotNull(master);
		ClassDetails child = findByTableName("CHILD");
		assertNotNull(child);
		// CHILD has one FK: MASTERREF -> MASTER
		List<FieldDetails> manyToOneFields = child.getFields().stream()
				.filter(f -> f.getDirectAnnotationUsage(ManyToOne.class) != null)
				.toList();
		assertEquals(1, manyToOneFields.size(), "should only be one ManyToOne FK");
		FieldDetails masterRefField = manyToOneFields.get(0);
		assertTrue(
				masterRefField.getType().determineRawClass().getName().endsWith("Master"),
				"ManyToOne should reference Master");
		JoinColumn joinColumn = masterRefField.getDirectAnnotationUsage(JoinColumn.class);
		assertNotNull(joinColumn, "ManyToOne field should have @JoinColumn");
		assertEquals("MASTERREF", joinColumn.name().toUpperCase());
	}

	@Test
	public void testExport() {
		SchemaExport schemaExport = new SchemaExport();
		final EnumSet<TargetType> targetTypes = EnumSet.noneOf( TargetType.class );
		targetTypes.add( TargetType.STDOUT );
		schemaExport.create(targetTypes, metadataDescriptor.createMetadata());
	}

	private ClassDetails findByTableName(String tableName) {
		for (ClassDetails cd : entities) {
			jakarta.persistence.Table tableAnn = cd.getDirectAnnotationUsage(jakarta.persistence.Table.class);
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
