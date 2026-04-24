/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.reveng.api.reveng.AssociationInfo;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.api.reveng.TableIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OverrideStrategyDelegateTest {

	private OverrideRepository repository;
	private TableIdentifier table;

	@BeforeEach
	void setUp() {
		repository = new OverrideRepository();
		table = TableIdentifier.create(null, null, "MY_TABLE");
	}

	private OverrideStrategyDelegate createDelegate(RevengStrategy delegate) {
		return new OverrideStrategyDelegate(repository, delegate);
	}

	@Test
	void testExcludeColumnWhenPresent() {
		repository.excludedColumns.add(new TableColumnKey(table, "SECRET_COL"));
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertTrue(delegate.excludeColumn(table, "SECRET_COL"));
	}

	@Test
	void testExcludeColumnWhenAbsent() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertFalse(delegate.excludeColumn(table, "NORMAL_COL"));
	}

	@Test
	void testTableToCompositeIdNameFromRepository() {
		repository.compositeIdNameForTable.put(table, "MyCompositeId");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("MyCompositeId", delegate.tableToCompositeIdName(table));
	}

	@Test
	void testTableToCompositeIdNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.tableToCompositeIdName(table));
	}

	@Test
	void testGetSchemaSelectionsFromRepository() {
		SchemaSelection selection = new SchemaSelection() {
			public String getMatchCatalog() { return null; }
			public String getMatchSchema() { return "PUBLIC"; }
			public String getMatchTable() { return null; }
		};
		repository.schemaSelections.add(selection);
		OverrideStrategyDelegate delegate = createDelegate(null);
		List<SchemaSelection> result = delegate.getSchemaSelections();
		assertEquals(1, result.size());
		assertEquals("PUBLIC", result.get(0).getMatchSchema());
	}

	@Test
	void testGetSchemaSelectionsFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.getSchemaSelections());
	}

	@Test
	void testColumnToPropertyNameFromRepository() {
		repository.propertyNameForColumn.put(
				new TableColumnKey(table, "FIRST_NAME"), "firstName");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("firstName", delegate.columnToPropertyName(table, "FIRST_NAME"));
	}

	@Test
	void testColumnToPropertyNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.columnToPropertyName(table, "FIRST_NAME"));
	}

	@Test
	void testTableToIdentifierPropertyNameFromRepository() {
		repository.propertyNameForPrimaryKey.put(table, "myId");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("myId", delegate.tableToIdentifierPropertyName(table));
	}

	@Test
	void testTableToIdentifierPropertyNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.tableToIdentifierPropertyName(table));
	}

	@Test
	void testGetTableIdentifierStrategyNameFromRepository() {
		repository.identifierStrategyForTable.put(table, "sequence");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("sequence", delegate.getTableIdentifierStrategyName(table));
	}

	@Test
	void testGetTableIdentifierStrategyNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.getTableIdentifierStrategyName(table));
	}

	@Test
	void testGetTableIdentifierPropertiesFromRepository() {
		Properties props = new Properties();
		props.setProperty("sequence_name", "my_seq");
		repository.identifierPropertiesForTable.put(table, props);
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("my_seq",
				delegate.getTableIdentifierProperties(table).getProperty("sequence_name"));
	}

	@Test
	void testGetTableIdentifierPropertiesFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.getTableIdentifierProperties(table));
	}

	@Test
	void testGetPrimaryKeyColumnNamesFromRepository() {
		repository.primaryKeyColumnsForTable.put(table, List.of("ID", "VERSION"));
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals(List.of("ID", "VERSION"), delegate.getPrimaryKeyColumnNames(table));
	}

	@Test
	void testGetPrimaryKeyColumnNamesFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.getPrimaryKeyColumnNames(table));
	}

	@Test
	void testForeignKeyToEntityNameFromRepository() {
		repository.foreignKeyToOneName.put("FK_DEPT", "department");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("department",
				delegate.foreignKeyToEntityName("FK_DEPT", table, List.of(), table, List.of(), false));
	}

	@Test
	void testForeignKeyToEntityNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.foreignKeyToEntityName("FK_DEPT", table, List.of(), table, List.of(), false));
	}

	@Test
	void testForeignKeyToInverseEntityNameFromRepository() {
		repository.foreignKeyToInverseName.put("FK_DEPT", "employees");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("employees",
				delegate.foreignKeyToInverseEntityName("FK_DEPT", table, List.of(), table, List.of(), false));
	}

	@Test
	void testForeignKeyToInverseEntityNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.foreignKeyToInverseEntityName("FK_DEPT", table, List.of(), table, List.of(), false));
	}

	@Test
	void testForeignKeyToCollectionNameFromRepository() {
		repository.foreignKeyToInverseName.put("FK_DEPT", "employees");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("employees",
				delegate.foreignKeyToCollectionName("FK_DEPT", table, List.of(), table, List.of(), false));
	}

	@Test
	void testForeignKeyToCollectionNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.foreignKeyToCollectionName("FK_DEPT", table, List.of(), table, List.of(), false));
	}

	@Test
	void testExcludeForeignKeyAsCollectionFromRepository() {
		repository.foreignKeyInverseExclude.put("FK_DEPT", true);
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertTrue(delegate.excludeForeignKeyAsCollection("FK_DEPT", table, List.of(), table, List.of()));
	}

	@Test
	void testExcludeForeignKeyAsCollectionFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertFalse(delegate.excludeForeignKeyAsCollection("FK_DEPT", table, List.of(), table, List.of()));
	}

	@Test
	void testExcludeForeignKeyAsManytoOneFromRepository() {
		repository.foreignKeyToOneExclude.put("FK_DEPT", true);
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertTrue(delegate.excludeForeignKeyAsManytoOne("FK_DEPT", table, List.of(), table, List.of()));
	}

	@Test
	void testExcludeForeignKeyAsManytoOneFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertFalse(delegate.excludeForeignKeyAsManytoOne("FK_DEPT", table, List.of(), table, List.of()));
	}

	@Test
	void testForeignKeyToAssociationInfoFromRepository() {
		AssociationInfo info = new AssociationInfo() {
			public String getCascade() { return "all"; }
			public String getFetch() { return "join"; }
			public Boolean getUpdate() { return true; }
			public Boolean getInsert() { return true; }
		};
		ForeignKey fk = new ForeignKey();
		fk.setName("FK_DEPT");
		repository.foreignKeyToEntityInfo.put("FK_DEPT", info);
		OverrideStrategyDelegate delegate = createDelegate(null);
		AssociationInfo result = delegate.foreignKeyToAssociationInfo(fk);
		assertNotNull(result);
		assertEquals("all", result.getCascade());
	}

	@Test
	void testForeignKeyToAssociationInfoFallsBackToDelegate() {
		ForeignKey fk = new ForeignKey();
		fk.setName("FK_UNKNOWN");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.foreignKeyToAssociationInfo(fk));
	}

	@Test
	void testForeignKeyToInverseAssociationInfoFromRepository() {
		AssociationInfo info = new AssociationInfo() {
			public String getCascade() { return "none"; }
			public String getFetch() { return "select"; }
			public Boolean getUpdate() { return false; }
			public Boolean getInsert() { return false; }
		};
		ForeignKey fk = new ForeignKey();
		fk.setName("FK_DEPT");
		repository.foreignKeyToInverseEntityInfo.put("FK_DEPT", info);
		OverrideStrategyDelegate delegate = createDelegate(null);
		AssociationInfo result = delegate.foreignKeyToInverseAssociationInfo(fk);
		assertNotNull(result);
		assertEquals("none", result.getCascade());
	}

	@Test
	void testForeignKeyToInverseAssociationInfoFallsBackToDelegate() {
		ForeignKey fk = new ForeignKey();
		fk.setName("FK_UNKNOWN");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.foreignKeyToInverseAssociationInfo(fk));
	}

	@Test
	void testGetForeignKeysFromRepository() {
		List<ForeignKey> fkList = new ArrayList<>();
		ForeignKey fk = new ForeignKey();
		fk.setName("FK_TEST");
		fkList.add(fk);
		repository.foreignKeys.put(table, fkList);
		OverrideStrategyDelegate delegate = createDelegate(null);
		List<ForeignKey> result = delegate.getForeignKeys(table);
		assertEquals(1, result.size());
		assertEquals("FK_TEST", result.get(0).getName());
	}

	@Test
	void testGetForeignKeysFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.getForeignKeys(table));
	}

	@Test
	void testColumnToHibernateTypeNameFromColumnOverride() {
		repository.typeForColumn.put(
				new TableColumnKey(table, "STATUS"), "string");
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertEquals("string",
				delegate.columnToHibernateTypeName(table, "STATUS", 12, 255, 0, 0, true, false));
	}

	@Test
	void testColumnToHibernateTypeNameFallsBackToDelegate() {
		OverrideStrategyDelegate delegate = createDelegate(null);
		assertNull(delegate.columnToHibernateTypeName(table, "STATUS", 12, 255, 0, 0, true, false));
	}
}
