/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.DefaultSchemaCatalog;

import jakarta.persistence.Table;

import org.hibernate.cfg.Environment;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.api.reveng.TableIdentifier;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.strategy.OverrideRepository;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
	public void testReadOnlySpecificSchema() {
		OverrideRepository or = new OverrideRepository();
		or.addSchemaSelection(createSchemaSelection("OVRTEST", null));
		RevengStrategy res = or.getReverseEngineeringStrategy(new DefaultStrategy());
		List<ClassDetails> entities = getEntities(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null));
		assertEquals(2, entities.size());
		ClassDetails catchild = entities.get(0);
		ClassDetails catmaster = entities.get(1);
		if (tableName(catchild).equals("CATMASTER")) {
			catchild = entities.get(1);
			catmaster = entities.get(0);
		}
		TableIdentifier masterid = createTableIdentifier(catmaster);
		TableIdentifier childid = createTableIdentifier(catchild);
		assertEquals(TableIdentifier.create(null, "OVRTEST", "CATMASTER"), masterid);
		assertEquals(TableIdentifier.create(null, "OVRTEST", "CATCHILD"), childid);
	}

	@Test
	public void testOverlapping() {
		OverrideRepository or = new OverrideRepository();
		or.addSchemaSelection(createSchemaSelection("OVRTEST", null));
		or.addSchemaSelection(createSchemaSelection(null, "MASTER"));
		or.addSchemaSelection(createSchemaSelection(null, "CHILD"));
		RevengStrategy res =
				or.getReverseEngineeringStrategy(new DefaultStrategy());
		List<ClassDetails> entities = getEntities(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null));
		Set<TableIdentifier> tables = new HashSet<>();
		for (ClassDetails entity : entities) {
			TableIdentifier tid = createTableIdentifier(entity);
			boolean added = tables.add(tid);
			if (!added)
				fail("duplicate table found for " + entity.getName());
		}
		assertEquals(4, tables.size());
	}

	@Test
	public void testUseDefault() {
		Properties properties = new Properties();
		properties.setProperty(Environment.DEFAULT_SCHEMA, "OVRTEST");
		properties.setProperty(Environment.DEFAULT_SCHEMA, "OVRTEST");
		List<ClassDetails> entities = getEntities(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, properties));
		assertEquals(2, entities.size());
		ClassDetails catchild = entities.get(0);
		ClassDetails catmaster = entities.get(1);
		if (tableName(catchild).equals("CATMASTER")) {
			catchild = entities.get(1);
			catmaster = entities.get(0);
		}
		TableIdentifier masterid = createTableIdentifier(catmaster);
		TableIdentifier childid = createTableIdentifier(catchild);
		assertEquals(TableIdentifier.create(null, null, "CATMASTER"), masterid, "jdbcreader has not nulled out according to default schema");
		assertEquals(TableIdentifier.create(null, null, "CATCHILD"), childid, "jdbcreader has not nulled out according to default schema");
	}

	private List<ClassDetails> getEntities(MetadataDescriptor descriptor) {
		return ((RevengMetadataDescriptor) descriptor).getEntityClassDetails();
	}

	private String tableName(ClassDetails classDetails) {
		Table tableAnn = classDetails.getDirectAnnotationUsage(Table.class);
		return tableAnn != null ? tableAnn.name().replace("`", "") : "";
	}

	private TableIdentifier createTableIdentifier(ClassDetails classDetails) {
		Table tableAnn = classDetails.getDirectAnnotationUsage(Table.class);
		if (tableAnn == null) {
			return null;
		}
		String catalog = tableAnn.catalog().replace("`", "");
		String schema = tableAnn.schema().replace("`", "");
		String name = tableAnn.name().replace("`", "");
		return TableIdentifier.create(
				catalog.isEmpty() ? null : catalog,
				schema.isEmpty() ? null : schema,
				name);
	}

	private SchemaSelection createSchemaSelection(String matchSchema, String matchTable) {
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
				return matchTable;
			}
		};
	}

}
