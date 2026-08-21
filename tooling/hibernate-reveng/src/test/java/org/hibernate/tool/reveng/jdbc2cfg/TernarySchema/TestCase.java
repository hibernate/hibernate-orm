/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.TernarySchema;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Set;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.internal.export.common.DefaultValueVisitor;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.internal.core.strategy.AbstractStrategy;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
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
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testTernaryModel() {
		assertMultiSchema(metadataDescriptor.createMetadata());
	}

	@Test
	public void testGeneration() {
		HbmExporter hme = new HbmExporter();
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
		assertMultiSchema(MetadataDescriptorFactory
				.createNativeDescriptor(null, files, null)
				.createMetadata());
	}

	private void assertMultiSchema(Metadata metadata) {
		JUnitUtil.assertIteratorContainsExactly(
				"There should be three entities!",
				metadata.getEntityBindings().iterator(),
				3);
		final PersistentClass role = metadata.getEntityBinding("Role");
		assertNotNull(role);
		PersistentClass member = metadata.getEntityBinding("Member");
		assertNotNull(member);
		PersistentClass plainRole = metadata.getEntityBinding("Plainrole");
		assertNotNull(plainRole);
		Property property = role.getProperty("members");
		assertEquals( "OTHERSCHEMA", role.getTable().getSchema() );
		assertNotNull(property);
		property.getValue().accept(new DefaultValueVisitor(true) {
			public Object accept(Set o) {
				assertEquals( "THIRDSCHEMA", o.getCollectionTable().getSchema() );
				return null;
			}
		});
		property = plainRole.getProperty("members");
		assertEquals( "OTHERSCHEMA", role.getTable().getSchema() );
		assertNotNull(property);
		property.getValue().accept(new DefaultValueVisitor(true) {
			public Object accept(Set o) {
				// TODO Investigate the ignored test: HBX-1410
				// For some reason the explicit schema 'HTT' is not reproduced in the many-to-many set in the hbm.xml files
				// Need to investigate the HBM generation and to try rebuild the metadata with the schema explicitly set
				// assertEquals("HTT", o.getCollectionTable().getSchema() );
				return null;
			}
		});
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
