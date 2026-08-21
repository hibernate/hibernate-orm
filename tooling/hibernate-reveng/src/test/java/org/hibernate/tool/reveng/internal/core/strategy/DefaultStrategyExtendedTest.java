/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultStrategyExtendedTest {

	private RevengStrategy strategy;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		strategy.setSettings(new RevengSettings(strategy));
	}

	@Test
	public void testExcludeTableReturnsFalse() {
		assertFalse(strategy.excludeTable(TableIdentifier.create("cat", "sch", "ANY_TABLE")));
	}

	@Test
	public void testExcludeColumnReturnsFalse() {
		assertFalse(strategy.excludeColumn(TableIdentifier.create(null, null, "T"), "any_column"));
	}

	@Test
	public void testGetForeignKeysReturnsEmpty() {
		List<?> fks = strategy.getForeignKeys(TableIdentifier.create(null, null, "T"));
		assertNotNull(fks);
		assertTrue(fks.isEmpty());
	}

	@Test
	public void testGetTableIdentifierStrategyNameReturnsNull() {
		assertNull(strategy.getTableIdentifierStrategyName(TableIdentifier.create(null, null, "T")));
	}

	@Test
	public void testGetTableIdentifierPropertiesReturnsEmpty() {
		Properties props = strategy.getTableIdentifierProperties(TableIdentifier.create(null, null, "T"));
		assertNotNull(props);
		assertTrue(props.isEmpty());
	}

	@Test
	public void testGetPrimaryKeyColumnNamesReturnsNull() {
		assertNull(strategy.getPrimaryKeyColumnNames(TableIdentifier.create(null, null, "T")));
	}

	@Test
	public void testClassNameToCompositeIdName() {
		assertEquals("PersonId", strategy.classNameToCompositeIdName("Person"));
		assertEquals("com.example.OrderId", strategy.classNameToCompositeIdName("com.example.Order"));
	}

	@Test
	public void testGetOptimisticLockColumnNameReturnsNull() {
		assertNull(strategy.getOptimisticLockColumnName(TableIdentifier.create(null, null, "T")));
	}

	@Test
	public void testUseColumnForOptimisticLockWithDetection() {
		RevengSettings settings = new RevengSettings(strategy).setDetectOptimisticLock(true);
		strategy.setSettings(settings);

		assertTrue(strategy.useColumnForOptimisticLock(TableIdentifier.create(null, null, "T"), "version"));
		assertTrue(strategy.useColumnForOptimisticLock(TableIdentifier.create(null, null, "T"), "VERSION"));
		assertTrue(strategy.useColumnForOptimisticLock(TableIdentifier.create(null, null, "T"), "timestamp"));
		assertTrue(strategy.useColumnForOptimisticLock(TableIdentifier.create(null, null, "T"), "dbtimestamp"));
		assertFalse(strategy.useColumnForOptimisticLock(TableIdentifier.create(null, null, "T"), "name"));
	}

	@Test
	public void testUseColumnForOptimisticLockDisabled() {
		RevengSettings settings = new RevengSettings(strategy).setDetectOptimisticLock(false);
		strategy.setSettings(settings);

		assertFalse(strategy.useColumnForOptimisticLock(TableIdentifier.create(null, null, "T"), "version"));
	}

	@Test
	public void testGetSchemaSelectionsReturnsNull() {
		assertNull(strategy.getSchemaSelections());
	}

	@Test
	public void testTableToIdentifierPropertyNameReturnsNull() {
		assertNull(strategy.tableToIdentifierPropertyName(TableIdentifier.create(null, null, "T")));
	}

	@Test
	public void testTableToCompositeIdNameReturnsNull() {
		assertNull(strategy.tableToCompositeIdName(TableIdentifier.create(null, null, "T")));
	}

	@Test
	public void testExcludeForeignKeyAsCollection() {
		RevengSettings settings = new RevengSettings(strategy).setCreateCollectionForForeignKey(true);
		strategy.setSettings(settings);
		assertFalse(strategy.excludeForeignKeyAsCollection("fk", null, null, null, null));

		settings = new RevengSettings(strategy).setCreateCollectionForForeignKey(false);
		strategy.setSettings(settings);
		assertTrue(strategy.excludeForeignKeyAsCollection("fk", null, null, null, null));
	}

	@Test
	public void testExcludeForeignKeyAsManytoOne() {
		RevengSettings settings = new RevengSettings(strategy).setCreateManyToOneForForeignKey(true);
		strategy.setSettings(settings);
		assertFalse(strategy.excludeForeignKeyAsManytoOne("fk", null, null, null, null));

		settings = new RevengSettings(strategy).setCreateManyToOneForForeignKey(false);
		strategy.setSettings(settings);
		assertTrue(strategy.excludeForeignKeyAsManytoOne("fk", null, null, null, null));
	}

	@Test
	public void testIsForeignKeyCollectionLazy() {
		assertTrue(strategy.isForeignKeyCollectionLazy("fk", null, null, null, null));
	}

	@Test
	public void testCloseDoesNotThrow() {
		strategy.close();
	}

	@Test
	public void testTableToMetaAttributesReturnsNull() {
		assertNull(strategy.tableToMetaAttributes(TableIdentifier.create(null, null, "T")));
	}

	@Test
	public void testColumnToMetaAttributesReturnsNull() {
		assertNull(strategy.columnToMetaAttributes(TableIdentifier.create(null, null, "T"), "col"));
	}

	@Test
	public void testForeignKeyToAssociationInfoReturnsNull() {
		assertNull(strategy.foreignKeyToAssociationInfo(null));
	}

	@Test
	public void testForeignKeyToInverseAssociationInfoReturnsNull() {
		assertNull(strategy.foreignKeyToInverseAssociationInfo(null));
	}

	@Test
	public void testTableToClassNameWithDefaultPackage() {
		RevengSettings settings = new RevengSettings(strategy).setDefaultPackageName("com.example");
		strategy.setSettings(settings);

		assertEquals("com.example.Person",
				strategy.tableToClassName(TableIdentifier.create(null, null, "PERSON")));
	}

	@Test
	public void testTableToClassNameWithEmptyPackage() {
		RevengSettings settings = new RevengSettings(strategy).setDefaultPackageName("");
		strategy.setSettings(settings);

		assertEquals("Person",
				strategy.tableToClassName(TableIdentifier.create(null, null, "PERSON")));
	}

	@Test
	public void testForeignKeyToInverseEntityName() {
		TableIdentifier from = TableIdentifier.create(null, null, "ORDER_ITEM");
		TableIdentifier to = TableIdentifier.create(null, null, "ORDER");

		assertEquals("order",
				strategy.foreignKeyToInverseEntityName("fk", from, Collections.emptyList(), to, Collections.emptyList(), true));
	}

	@Test
	public void testIsForeignKeyCollectionInverseNullTable() {
		assertTrue(strategy.isForeignKeyCollectionInverse("fk", null, null, null, null));
	}

	@Test
	public void testColumnToPropertyNameWithDigits() {
		assertEquals("address1", strategy.columnToPropertyName(null, "ADDRESS_1"));
		assertEquals("address2Line", strategy.columnToPropertyName(null, "ADDRESS_2_LINE"));
	}
}
