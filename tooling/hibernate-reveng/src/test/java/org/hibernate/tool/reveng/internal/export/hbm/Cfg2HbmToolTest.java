/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Cfg2HbmToolTest {

	private MetadataBuildingContext createMetadataBuildingContext() {
		return (MetadataBuildingContext) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[]{MetadataBuildingContext.class},
				(proxy, method, args) -> null);
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesNull() {
		assertNull(Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(null, new Properties()));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesEmpty() {
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(new Properties(), new Properties());
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesFiltersTarget() {
		Properties props = new Properties();
		props.put("target_table", "some_table");
		props.put("sequence_name", "my_seq");
		Properties env = new Properties();
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("target_table"));
		assertTrue(result.containsKey("sequence_name"));
		assertEquals("my_seq", result.getProperty("sequence_name"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesFiltersDefaultSchema() {
		Properties props = new Properties();
		props.put("schema", "public");
		Properties env = new Properties();
		env.put("hibernate.default_schema", "public");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("schema"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesKeepsNonDefaultSchema() {
		Properties props = new Properties();
		props.put("schema", "custom");
		Properties env = new Properties();
		env.put("hibernate.default_schema", "public");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertTrue(result.containsKey("schema"));
		assertEquals("custom", result.getProperty("schema"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesFiltersDefaultCatalog() {
		Properties props = new Properties();
		props.put("catalog", "defaultCat");
		Properties env = new Properties();
		env.put("hibernate.default_catalog", "defaultCat");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("catalog"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesKeepsNonDefaultCatalog() {
		Properties props = new Properties();
		props.put("catalog", "customCat");
		Properties env = new Properties();
		env.put("hibernate.default_catalog", "defaultCat");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertTrue(result.containsKey("catalog"));
		assertEquals("customCat", result.getProperty("catalog"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesMixedKeys() {
		Properties props = new Properties();
		props.put("target_column", "id");
		props.put("schema", "public");
		props.put("catalog", "myCat");
		props.put("sequence_name", "seq1");
		props.put("initial_value", "1");
		Properties env = new Properties();
		env.put("hibernate.default_schema", "public");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("target_column"));
		assertFalse(result.containsKey("schema"));
		assertTrue(result.containsKey("catalog"));
		assertTrue(result.containsKey("sequence_name"));
		assertTrue(result.containsKey("initial_value"));
	}

	// --- Type-check methods ---

	@Test
	public void testIsJoinedSubclass() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		PersistentClass root = new RootClass(mdbc);
		assertFalse(c2h.isJoinedSubclass(root));
		assertTrue(c2h.isJoinedSubclass(new JoinedSubclass(root, mdbc)));
		assertFalse(c2h.isJoinedSubclass(new SingleTableSubclass(root, mdbc)));
	}

	@Test
	public void testIsSubclass() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		PersistentClass root = new RootClass(mdbc);
		assertFalse(c2h.isSubclass(root));
		assertTrue(c2h.isSubclass(new JoinedSubclass(root, mdbc)));
		assertTrue(c2h.isSubclass(new SingleTableSubclass(root, mdbc)));
		assertTrue(c2h.isSubclass(new UnionSubclass(root, mdbc)));
		assertTrue(c2h.isSubclass(new Subclass(root, mdbc)));
	}

	@Test
	public void testNeedsDiscriminator() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		PersistentClass root = new RootClass(mdbc);
		assertFalse(c2h.needsDiscriminator(root));
		assertTrue(c2h.needsDiscriminator(new SingleTableSubclass(root, mdbc)));
		assertTrue(c2h.needsDiscriminator(new Subclass(root, mdbc)));
		assertFalse(c2h.needsDiscriminator(new JoinedSubclass(root, mdbc)));
		assertFalse(c2h.needsDiscriminator(new UnionSubclass(root, mdbc)));
	}

	@Test
	public void testNeedsDiscriminatorElement() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		RootClass root = new RootClass(mdbc);
		assertFalse(c2h.needsDiscriminatorElement(root)); // no discriminator set
	}


	@Test
	public void testColumnAttributes() {
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		Column col = new Column("test_col");
		col.setLength(255L);
		String attrs = c2h.columnAttributes(col);
		assertTrue(attrs.contains("length=\"255\""));
	}

	@Test
	public void testColumnAttributesWithPrecisionAndScale() {
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		Column col = new Column("price");
		col.setPrecision(10);
		col.setScale(2);
		String attrs = c2h.columnAttributes(col);
		assertTrue(attrs.contains("precision=\"10\""));
		assertTrue(attrs.contains("scale=\"2\""));
		assertFalse(attrs.contains("length="));
	}

	@Test
	public void testColumnAttributesNotNullAndUnique() {
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		Column col = new Column("id");
		col.setNullable(false);
		col.setUnique(true);
		String attrs = c2h.columnAttributes(col, false);
		assertTrue(attrs.contains("not-null=\"true\""));
		assertTrue(attrs.contains("unique=\"true\""));
		// Primary key column should suppress not-null and unique
		String pkAttrs = c2h.columnAttributes(col, true);
		assertFalse(pkAttrs.contains("not-null"));
		assertFalse(pkAttrs.contains("unique"));
	}

	@Test
	public void testColumnAttributesWithSqlType() {
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		Column col = new Column("data");
		col.setSqlType("varchar(100)");
		String attrs = c2h.columnAttributes(col);
		assertTrue(attrs.contains("sql-type=\"varchar(100)\""));
	}

	@Test
	public void testGetTagForPersistentClass() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		PersistentClass root = new RootClass(mdbc);
		assertEquals("class", c2h.getTag(root));
		assertEquals("joined-subclass", c2h.getTag(new JoinedSubclass(root, mdbc)));
		assertEquals("union-subclass", c2h.getTag(new UnionSubclass(root, mdbc)));
		assertEquals("subclass", c2h.getTag(new SingleTableSubclass(root, mdbc)));
		assertEquals("subclass", c2h.getTag(new Subclass(root, mdbc)));
	}

	@Test
	public void testIsClassLevelOptimisticLockMode() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		RootClass root = new RootClass(mdbc);
		root.setOptimisticLockStyle(OptimisticLockStyle.VERSION);
		assertFalse(c2h.isClassLevelOptimisticLockMode(root));
		root.setOptimisticLockStyle(OptimisticLockStyle.DIRTY);
		assertTrue(c2h.isClassLevelOptimisticLockMode(root));
	}

	@Test
	public void testGetClassLevelOptimisticLockMode() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		RootClass root = new RootClass(mdbc);
		assertEquals("version", c2h.getClassLevelOptimisticLockMode(root));
		root.setOptimisticLockStyle(OptimisticLockStyle.DIRTY);
		assertEquals("dirty", c2h.getClassLevelOptimisticLockMode(root));
		root.setOptimisticLockStyle(OptimisticLockStyle.ALL);
		assertEquals("all", c2h.getClassLevelOptimisticLockMode(root));
		root.setOptimisticLockStyle(OptimisticLockStyle.NONE);
		assertEquals("none", c2h.getClassLevelOptimisticLockMode(root));
	}

	@Test
	public void testIsImportData() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
				.applySetting("hibernate.connection.driver_class", "org.h2.Driver")
				.applySetting("hibernate.connection.url", "jdbc:h2:mem:cfg2hbm_import_test")
				.applySetting("hibernate.connection.username", "sa")
				.applySetting("hibernate.connection.password", "")
				.applySetting("hibernate.default_schema", "")
				.applySetting("hibernate.default_catalog", "")
				.build();
		Metadata md = new MetadataSources(ssr)
				.addResource("org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")
				.buildMetadata();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		// Metadata with entity binding has imports
		assertTrue(c2h.isImportData(md));
		assertFalse(c2h.isNamedQueries(md));
		assertFalse(c2h.isNamedSQLQueries(md));
		assertFalse(c2h.isFilterDefinitions(md));
		StandardServiceRegistryBuilder.destroy(ssr);
	}
}
