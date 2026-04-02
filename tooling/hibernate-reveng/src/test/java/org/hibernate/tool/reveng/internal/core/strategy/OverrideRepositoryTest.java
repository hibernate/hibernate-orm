/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OverrideRepositoryTest {

	private OverrideRepository parseXml(String xml) {
		OverrideRepository repo = new OverrideRepository();
		repo.addInputStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		return repo;
	}

	private RevengStrategy createStrategy(OverrideRepository repo) {
		RevengStrategy strategy = repo.getReverseEngineeringStrategy(new DefaultStrategy());
		strategy.setSettings(new RevengSettings(strategy));
		return strategy;
	}

	@Test
	public void testEmptyRepository() {
		OverrideRepository repo = new OverrideRepository();
		RevengStrategy strategy = createStrategy(repo);
		assertNotNull(strategy);
	}

	@Test
	public void testTableFilterExclude() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table-filter match-catalog=\".*\" match-schema=\".*\" match-name=\"AUDIT.*\" exclude=\"true\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertTrue(strategy.excludeTable(TableIdentifier.create("cat", "sch", "AUDIT_LOG")));
		assertTrue(strategy.excludeTable(TableIdentifier.create("cat", "sch", "AUDIT_TRAIL")));
	}

	@Test
	public void testTableFilterInclude() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table-filter match-catalog=\".*\" match-schema=\".*\" match-name=\"PERSON\" exclude=\"false\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertFalse(strategy.excludeTable(TableIdentifier.create("cat", "sch", "PERSON")));
	}

	@Test
	public void testTableFilterWithPackage() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table-filter match-catalog=\".*\" match-schema=\".*\" match-name=\".*\" package=\"com.example.model\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		String className = strategy.tableToClassName(TableIdentifier.create("cat", "sch", "PERSON"));
		assertTrue(className.startsWith("com.example.model."));
	}

	@Test
	public void testSchemaSelection() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<schema-selection match-catalog=\"MY_CAT\" match-schema=\"MY_SCHEMA\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		List<RevengStrategy.SchemaSelection> selections = strategy.getSchemaSelections();
		assertNotNull(selections);
		assertFalse(selections.isEmpty());
	}

	@Test
	public void testTableClassName() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\" class=\"com.example.Person\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertEquals("com.example.Person", strategy.tableToClassName(TableIdentifier.create(null, null, "PERSON")));
	}

	@Test
	public void testTableClassNameWithoutPackage() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\" class=\"PersonEntity\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertEquals("PersonEntity", strategy.tableToClassName(TableIdentifier.create(null, null, "PERSON")));
	}

	@Test
	public void testTableClassNameWithTableFilterPackage() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table-filter match-catalog=\".*\" match-schema=\".*\" match-name=\".*\" package=\"com.example\"/>"
				+ "<table name=\"PERSON\" class=\"PersonEntity\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertEquals("com.example.PersonEntity", strategy.tableToClassName(TableIdentifier.create("cat", "sch", "PERSON")));
	}

	@Test
	public void testColumnPropertyName() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <column name=\"FIRST_NAME\" property=\"firstName\"/>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		assertEquals("firstName", strategy.columnToPropertyName(ti, "FIRST_NAME"));
	}

	@Test
	public void testColumnType() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <column name=\"STATUS\" type=\"string\"/>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		assertEquals("string", strategy.columnToHibernateTypeName(ti, "STATUS", Types.VARCHAR, 255, 0, 0, true, false));
	}

	@Test
	public void testColumnExclude() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <column name=\"INTERNAL\" exclude=\"true\"/>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		assertTrue(strategy.excludeColumn(ti, "INTERNAL"));
		assertFalse(strategy.excludeColumn(ti, "FIRST_NAME"));
	}

	@Test
	public void testPrimaryKeyColumns() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <primary-key>"
				+ "    <key-column name=\"PERSON_ID\"/>"
				+ "  </primary-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		List<String> pkCols = strategy.getPrimaryKeyColumnNames(ti);
		assertNotNull(pkCols);
		assertEquals(1, pkCols.size());
		assertEquals("PERSON_ID", pkCols.get(0));
	}

	@Test
	public void testPrimaryKeyPropertyName() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <primary-key property=\"personId\">"
				+ "    <key-column name=\"PERSON_ID\"/>"
				+ "  </primary-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		assertEquals("personId", strategy.tableToIdentifierPropertyName(ti));
	}

	@Test
	public void testPrimaryKeyGeneratorClass() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <primary-key>"
				+ "    <generator class=\"sequence\"/>"
				+ "    <key-column name=\"PERSON_ID\"/>"
				+ "  </primary-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		assertEquals("sequence", strategy.getTableIdentifierStrategyName(ti));
	}

	@Test
	public void testTypeMapping() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<type-mapping>"
				+ "  <sql-type jdbc-type=\"VARCHAR\" hibernate-type=\"string\"/>"
				+ "</type-mapping>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertEquals("string", strategy.columnToHibernateTypeName(null, "col", Types.VARCHAR, 255, 0, 0, true, false));
	}

	@Test
	public void testTypeMappingWithLength() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<type-mapping>"
				+ "  <sql-type jdbc-type=\"VARCHAR\" length=\"1\" hibernate-type=\"character\"/>"
				+ "  <sql-type jdbc-type=\"VARCHAR\" hibernate-type=\"string\"/>"
				+ "</type-mapping>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertEquals("character", strategy.columnToHibernateTypeName(null, "col", Types.VARCHAR, 1, 0, 0, true, false));
		assertEquals("string", strategy.columnToHibernateTypeName(null, "col", Types.VARCHAR, 255, 0, 0, true, false));
	}

	@Test
	public void testForeignKeyPropertyNames() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <foreign-key constraint-name=\"FK_ADDR\" foreign-table=\"ADDRESS\">"
				+ "    <column-ref local-column=\"ADDR_ID\" foreign-column=\"ID\"/>"
				+ "    <many-to-one property=\"homeAddress\"/>"
				+ "    <set property=\"residents\"/>"
				+ "  </foreign-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier from = TableIdentifier.create(null, null, "PERSON");
		TableIdentifier to = TableIdentifier.create(null, null, "ADDRESS");

		assertEquals("homeAddress", strategy.foreignKeyToEntityName("FK_ADDR", from, Collections.emptyList(), to, Collections.emptyList(), true));
		assertEquals("residents", strategy.foreignKeyToCollectionName("FK_ADDR", from, Collections.emptyList(), to, Collections.emptyList(), true));
	}

	@Test
	public void testForeignKeyExclude() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <foreign-key constraint-name=\"FK_ADDR\" foreign-table=\"ADDRESS\">"
				+ "    <column-ref local-column=\"ADDR_ID\" foreign-column=\"ID\"/>"
				+ "    <many-to-one exclude=\"true\"/>"
				+ "  </foreign-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier from = TableIdentifier.create(null, null, "PERSON");
		TableIdentifier to = TableIdentifier.create(null, null, "ADDRESS");

		assertTrue(strategy.excludeForeignKeyAsManytoOne("FK_ADDR", from, Collections.emptyList(), to, Collections.emptyList()));
	}

	@Test
	public void testForeignKeyInverseExclude() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <foreign-key constraint-name=\"FK_ADDR\" foreign-table=\"ADDRESS\">"
				+ "    <column-ref local-column=\"ADDR_ID\" foreign-column=\"ID\"/>"
				+ "    <many-to-one property=\"addr\"/>"
				+ "    <set exclude=\"true\"/>"
				+ "  </foreign-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier from = TableIdentifier.create(null, null, "PERSON");
		TableIdentifier to = TableIdentifier.create(null, null, "ADDRESS");

		assertTrue(strategy.excludeForeignKeyAsCollection("FK_ADDR", from, Collections.emptyList(), to, Collections.emptyList()));
	}

	@Test
	public void testAddTypeMappingProgrammatically() {
		OverrideRepository repo = new OverrideRepository();
		SQLTypeMapping mapping = new SQLTypeMapping(Types.BIT);
		mapping.setHibernateType("boolean");
		repo.addTypeMapping(mapping);

		RevengStrategy strategy = createStrategy(repo);
		assertEquals("boolean", strategy.columnToHibernateTypeName(null, "col", Types.BIT, 0, 0, 0, false, false));
	}

	@Test
	public void testAddTableFilterProgrammatically() {
		OverrideRepository repo = new OverrideRepository();
		TableFilter filter = new TableFilter();
		filter.setMatchName("TMP.*");
		filter.setExclude(Boolean.TRUE);
		repo.addTableFilter(filter);

		RevengStrategy strategy = createStrategy(repo);
		assertTrue(strategy.excludeTable(TableIdentifier.create("cat", "sch", "TMP_DATA")));
	}

	@Test
	public void testTypeMappingKeyEqualsAndHashCode() {
		OverrideRepository.TypeMappingKey a = new OverrideRepository.TypeMappingKey(Types.VARCHAR, 255);
		OverrideRepository.TypeMappingKey b = new OverrideRepository.TypeMappingKey(Types.VARCHAR, 255);
		OverrideRepository.TypeMappingKey c = new OverrideRepository.TypeMappingKey(Types.INTEGER, 255);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(c));
		assertFalse(a.equals(null));
		assertNotNull(a.toString());
	}

	@Test
	public void testTableColumnKeyEqualsAndHashCode() {
		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		OverrideRepository.TableColumnKey a = new OverrideRepository.TableColumnKey(ti, "NAME");
		OverrideRepository.TableColumnKey b = new OverrideRepository.TableColumnKey(ti, "NAME");
		OverrideRepository.TableColumnKey c = new OverrideRepository.TableColumnKey(ti, "AGE");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(c));
		assertFalse(a.equals(null));
		assertFalse(a.equals("string"));
		assertTrue(a.equals(a));
	}

	@Test
	public void testTableColumnKeyWithNulls() {
		OverrideRepository.TableColumnKey a = new OverrideRepository.TableColumnKey(null, null);
		OverrideRepository.TableColumnKey b = new OverrideRepository.TableColumnKey(null, null);
		assertEquals(a, b);

		TableIdentifier ti = TableIdentifier.create(null, null, "T");
		OverrideRepository.TableColumnKey c = new OverrideRepository.TableColumnKey(ti, "C");
		assertFalse(a.equals(c));
		assertFalse(c.equals(a));
	}

	@Test
	public void testCompositeIdName() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"ORDER_ITEM\">"
				+ "  <primary-key id-class=\"OrderItemPK\">"
				+ "    <key-column name=\"ORDER_ID\"/>"
				+ "    <key-column name=\"ITEM_ID\"/>"
				+ "  </primary-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "ORDER_ITEM");
		assertEquals("OrderItemPK", strategy.tableToCompositeIdName(ti));
	}

	@Test
	public void testMultipleTableFilters() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table-filter match-catalog=\".*\" match-schema=\".*\" match-name=\"INCLUDE_ME\" exclude=\"false\"/>"
				+ "<table-filter match-catalog=\".*\" match-schema=\".*\" match-name=\".*\" exclude=\"true\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertFalse(strategy.excludeTable(TableIdentifier.create("cat", "sch", "INCLUDE_ME")));
		assertTrue(strategy.excludeTable(TableIdentifier.create("cat", "sch", "OTHER")));
	}

	@Test
	public void testTableWithCatalogAndSchema() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table catalog=\"MY_CAT\" schema=\"MY_SCH\" name=\"PERSON\" class=\"com.example.Person\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertEquals("com.example.Person",
				strategy.tableToClassName(TableIdentifier.create("MY_CAT", "MY_SCH", "PERSON")));
	}

	@Test
	public void testTableFilterCatalogAndSchema() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table-filter match-catalog=\"PROD\" match-schema=\"PUBLIC\" match-name=\".*\" exclude=\"true\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertTrue(strategy.excludeTable(TableIdentifier.create("PROD", "PUBLIC", "DATA")));
		assertFalse(strategy.excludeTable(TableIdentifier.create("DEV", "PUBLIC", "DATA")));
	}

	@Test
	public void testGetReverseEngineeringStrategyDeprecated() {
		OverrideRepository repo = new OverrideRepository();
		@SuppressWarnings("deprecation")
		RevengStrategy strategy = repo.getReverseEngineeringStrategy();
		assertNotNull(strategy);
	}

	@Test
	public void testColumnUnchangedWhenNoOverride() {
		OverrideRepository repo = new OverrideRepository();
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		assertEquals("firstName", strategy.columnToPropertyName(ti, "FIRST_NAME"));
	}

	@Test
	public void testSchemaSelectionMatchTable() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<schema-selection match-catalog=\"MY_CAT\" match-schema=\"MY_SCHEMA\" match-table=\"PERSON\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		List<RevengStrategy.SchemaSelection> selections = strategy.getSchemaSelections();
		assertEquals(1, selections.size());
		assertEquals("MY_CAT", selections.get(0).getMatchCatalog());
		assertEquals("MY_SCHEMA", selections.get(0).getMatchSchema());
		assertEquals("PERSON", selections.get(0).getMatchTable());
	}

	@Test
	public void testMultipleSchemaSelections() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<schema-selection match-catalog=\"CAT1\"/>"
				+ "<schema-selection match-catalog=\"CAT2\"/>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		List<RevengStrategy.SchemaSelection> selections = strategy.getSchemaSelections();
		assertEquals(2, selections.size());
	}

	@Test
	public void testTypeMappingWithPrecisionAndScale() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<type-mapping>"
				+ "  <sql-type jdbc-type=\"NUMERIC\" precision=\"10\" scale=\"2\" hibernate-type=\"java.math.BigDecimal\"/>"
				+ "</type-mapping>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		assertEquals("java.math.BigDecimal",
				strategy.columnToHibernateTypeName(null, "col", Types.NUMERIC, 0, 10, 2, true, false));
	}

	@Test
	public void testPrimaryKeyWithGeneratorParams() {
		String xml = "<hibernate-reverse-engineering>"
				+ "<table name=\"PERSON\">"
				+ "  <primary-key>"
				+ "    <generator class=\"sequence\">"
				+ "      <param name=\"sequence_name\">person_seq</param>"
				+ "    </generator>"
				+ "    <key-column name=\"ID\"/>"
				+ "  </primary-key>"
				+ "</table>"
				+ "</hibernate-reverse-engineering>";
		OverrideRepository repo = parseXml(xml);
		RevengStrategy strategy = createStrategy(repo);

		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");
		assertEquals("sequence", strategy.getTableIdentifierStrategyName(ti));
		assertNotNull(strategy.getTableIdentifierProperties(ti));
		assertEquals("person_seq", strategy.getTableIdentifierProperties(ti).getProperty("sequence_name"));
	}
}
