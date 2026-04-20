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
package org.hibernate.tool.jdbc2cfg.TernarySchema;

import jakarta.persistence.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.strategy.AbstractStrategy;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@TempDir
	public File outputFolder = new File("output");

	private MetadataDescriptor metadataDescriptor = null;
	private List<ClassDetails> entities = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		AbstractStrategy c = new AbstractStrategy() {
			public List<SchemaSelection> getSchemaSelections() {
				List<SchemaSelection> selections = new ArrayList<>();
				selections.add(createSchemaSelection("HTT"));
				selections.add(createSchemaSelection("OTHERSCHEMA"));
				selections.add(createSchemaSelection("THIRDSCHEMA"));
				return selections;
			}
		};
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(c, null);
		entities = ((RevengMetadataDescriptor) metadataDescriptor).getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testTernaryModel() {
		assertMultiSchema(entities);
	}

	@Test
	public void testGeneration() throws Exception {
		Exporter hme = ExporterFactory.createExporter(ExporterType.HBM);
		hme.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hme.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		hme.start();
		JUnitUtil.assertIsNonEmptyFile( new File(outputFolder, "Role.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputFolder, "Member.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputFolder, "Plainrole.hbm.xml") );
		assertEquals(3, Objects.requireNonNull( outputFolder.listFiles() ).length);
		File[] files = new File[3];
		files[0] = new File(outputFolder, "Role.hbm.xml");
		files[1] = new File(outputFolder, "Member.hbm.xml");
		files[2] = new File(outputFolder, "Plainrole.hbm.xml");
		// Verify the generated HBM files are readable XML
		javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory
				.newInstance().newDocumentBuilder();
		for (File file : files) {
			assertNotNull(db.parse(file));
		}
	}

	private void assertMultiSchema(List<ClassDetails> entityList) {
		// Filter to only @Entity annotated classes (exclude embeddables)
		List<ClassDetails> entityOnly = entityList.stream()
				.filter(cd -> cd.hasDirectAnnotationUsage(jakarta.persistence.Entity.class))
				.toList();
		assertEquals(3, entityOnly.size(), "There should be three entities!");

		ClassDetails role = findEntity(entityOnly, "Role");
		assertNotNull(role);
		ClassDetails member = findEntity(entityOnly, "Member");
		assertNotNull(member);
		ClassDetails plainRole = findEntity(entityOnly, "Plainrole");
		assertNotNull(plainRole);

		// Check @Table schema values
		Table roleTable = role.getDirectAnnotationUsage(Table.class);
		assertNotNull(roleTable);
		assertEquals("OTHERSCHEMA", roleTable.schema());

		assertNotNull(findField(role, "members"));

		Table plainRoleTable = plainRole.getDirectAnnotationUsage(Table.class);
		assertNotNull(plainRoleTable);

		assertNotNull(findField(plainRole, "members"));
	}

	private ClassDetails findEntity(List<ClassDetails> entities, String name) {
		return entities.stream()
				.filter(cd -> {
					String className = cd.getName();
					if (className.startsWith("`") && className.endsWith("`")) {
						className = className.substring(1, className.length() - 1);
					}
					return className.equals(name) || className.equalsIgnoreCase(name);
				})
				.findFirst()
				.orElse(null);
	}

	private FieldDetails findField(ClassDetails classDetails, String fieldName) {
		return classDetails.getFields().stream()
				.filter(f -> f.getName().equals(fieldName))
				.findFirst()
				.orElse(null);
	}

	private SchemaSelection createSchemaSelection(String matchSchema) {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return null;
			}
			@Override
			public String getMatchSchema() {
				return matchSchema;
			}
			@Override
			public String getMatchTable() {
				return null;
			}
		};
	}
}
