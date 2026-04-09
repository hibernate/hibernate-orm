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
package org.hibernate.tool.jdbc2cfg.OverrideBinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.internal.reveng.strategy.SQLTypeMapping;
import org.hibernate.tool.internal.reveng.strategy.TableFilter;
import org.hibernate.tool.internal.reveng.util.EnhancedValue;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author max
 * @author koen
 */
public class TestCase {
	
	private static final String OVERRIDE_TEST_REVENG_XML = "org/hibernate/tool/jdbc2cfg/OverrideBinder/overridetest.reveng.xml";
	private static final String TEST_REVENG_XML = "org/hibernate/tool/jdbc2cfg/OverrideBinder/test.reveng.xml";
	private static final String DOC_REVENG_XML = "org/hibernate/tool/jdbc2cfg/OverrideBinder/docexample.reveng.xml";
	private static final String SCHEMA_REVENG_XML = "org/hibernate/tool/jdbc2cfg/OverrideBinder/schemaselection.reveng.xml";

	private Metadata metadata = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		OverrideRepository or = new OverrideRepository();
		or.addResource(OVERRIDE_TEST_REVENG_XML);
		RevengStrategy res = or.getReverseEngineeringStrategy(
				new DefaultStrategy() );
		metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null)
				.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testReadTypeMappings() {
		OverrideRepository or = new OverrideRepository();
				
		or.addResource(TEST_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(null);

		assertEquals("int", repository.columnToHibernateTypeName(null, null, Types.INTEGER, 5, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
		assertEquals("long", repository.columnToHibernateTypeName(null, null, Types.INTEGER, SQLTypeMapping.UNKNOWN_LENGTH, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
		assertEquals("byte[]", repository.columnToHibernateTypeName(null, null, Types.BIGINT, SQLTypeMapping.UNKNOWN_LENGTH, 5, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
		assertEquals("java.math.BigInteger", repository.columnToHibernateTypeName(null, null, Types.BIGINT, SQLTypeMapping.UNKNOWN_LENGTH, 2, 3, false, false) );
		assertEquals("string", repository.columnToHibernateTypeName(null, null, Types.CHAR, 1, 10, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
		//Assert.assertEquals("string", repository.jdbcToHibernateType(Types.CHAR, 1, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE) );
		
		assertEquals("Long", repository.columnToHibernateTypeName(null, null, Types.NUMERIC, 1, 10, 0, false, false) );
		assertEquals("java.lang.Long", repository.columnToHibernateTypeName(null, null, Types.NUMERIC, 1, 10, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
		assertEquals("java.lang.Long", repository.columnToHibernateTypeName(null, null, Types.NUMERIC, 1, 10, 43, false, false) );
		
		// nullability
		assertEquals("nonnull-float", repository.columnToHibernateTypeName(null, null, Types.FLOAT, SQLTypeMapping.UNKNOWN_LENGTH, SQLTypeMapping.UNKNOWN_PRECISION,17,false, false) );
		assertEquals("null-float", repository.columnToHibernateTypeName(null, null, Types.FLOAT, SQLTypeMapping.UNKNOWN_LENGTH, SQLTypeMapping.UNKNOWN_PRECISION,17,true, false) );
		
		assertEquals("onlynotnull", repository.columnToHibernateTypeName(null, null, Types.FLOAT, SQLTypeMapping.UNKNOWN_LENGTH, SQLTypeMapping.UNKNOWN_PRECISION,SQLTypeMapping.UNKNOWN_SCALE,false, false) );
		assertEquals("donotcare", repository.columnToHibernateTypeName(null, null, Types.FLOAT, SQLTypeMapping.UNKNOWN_LENGTH, SQLTypeMapping.UNKNOWN_PRECISION,SQLTypeMapping.UNKNOWN_SCALE,true, false) );
		
		
	}
	
	@Test
	public void testDocExample() {
		OverrideRepository or = new OverrideRepository();
		
		or.addResource(DOC_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(new DefaultStrategy());

		assertEquals("int", repository.columnToHibernateTypeName(null, "ID", Types.INTEGER, SQLTypeMapping.UNKNOWN_LENGTH, 10, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
		assertEquals("your.package.TrimStringUserType", repository.columnToHibernateTypeName(null, "NAME", Types.VARCHAR, 30, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, true, false) );
		assertEquals("char", repository.columnToHibernateTypeName(null, "INITIAL", Types.VARCHAR, 1, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, true, false) );
		assertEquals("java.lang.Character", repository.columnToHibernateTypeName(null, "CODE", Types.VARCHAR, 1, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
		assertEquals("big_decimal", repository.columnToHibernateTypeName(null, "SALARY", Types.NUMERIC, SQLTypeMapping.UNKNOWN_LENGTH, 15, SQLTypeMapping.UNKNOWN_SCALE, true, false) );
		assertEquals("java.lang.Long", repository.columnToHibernateTypeName(null, "AGE", Types.NUMERIC, SQLTypeMapping.UNKNOWN_LENGTH, 3, SQLTypeMapping.UNKNOWN_SCALE, true, false) );
		
	}
	
	@Test
	public void testSchemaSelection() {
		OverrideRepository or = new OverrideRepository();
		
		or.addResource(SCHEMA_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(new DefaultStrategy());

		List<?> schemaSelectors = repository.getSchemaSelections();
		
		assertNotNull(schemaSelectors);
		assertEquals(4,schemaSelectors.size());
		
		SchemaSelection ss;
		ss = (SchemaSelection) schemaSelectors.get(0);
        assertNull(ss.getMatchCatalog());
        assertNull(ss.getMatchSchema());
        assertNull(ss.getMatchTable());
		
		ss = (SchemaSelection) schemaSelectors.get(1);
        assertNull(ss.getMatchCatalog());
		assertEquals("OVRTEST",ss.getMatchSchema());
        assertNull(ss.getMatchTable());
		
		ss = (SchemaSelection) schemaSelectors.get(2);
		assertEquals("UBERCATALOG",ss.getMatchCatalog());
		assertEquals("OVRTEST",ss.getMatchSchema());
        assertNull(ss.getMatchTable());
		
		ss = (SchemaSelection) schemaSelectors.get(3);
		assertEquals("PUBLIC.*",ss.getMatchCatalog());
		assertEquals("OVRTEST",ss.getMatchSchema());
		assertEquals(".*",ss.getMatchTable());
		
		OverrideRepository ox = new OverrideRepository();
		ox.addSchemaSelection(createDummySchemaSelection());
		RevengStrategy strategy = ox.getReverseEngineeringStrategy(new DefaultStrategy());
		Metadata md = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(strategy, null)
				.createMetadata();
		
		Iterator<Table> tableMappings = md.collectTableMappings().iterator();
		Table t = tableMappings.next();
		assertEquals("DUMMY", t.getName());
		assertFalse(tableMappings.hasNext());
	}

	@Test
	public void testColumnTypeMappings() {
		OverrideRepository or = new OverrideRepository();
		
		or.addResource(OVERRIDE_TEST_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(null);

		assertNull(repository.columnToHibernateTypeName(TableIdentifier.create(null, null, "blah"), "bogus",0,0,0,0, false, false));
		assertNull(repository.columnToHibernateTypeName(TableIdentifier.create(null, null, "ORDERS"), "CUSTID",0,0,0,0, false, false));
		assertEquals("string", repository.columnToHibernateTypeName(TableIdentifier.create(null, null, "ORDERS"), "NAME",0,0,0,0, false, false));
		
		PersistentClass classMapping = metadata.getEntityBinding("Orders");
		
		Property property = classMapping.getProperty("completed");		
		assertEquals("boolean", ((SimpleValue)property.getValue()).getTypeName(), "boolean because of not null");
		
		property = classMapping.getProperty("verified");
		assertEquals("java.lang.Boolean", ((SimpleValue)property.getValue()).getTypeName(),"java.lang.Boolean because of null");
		
		classMapping = metadata.getEntityBinding("MiscTypes");
		
		property = classMapping.getIdentifierProperty();
		
		assertFalse(property.getValue().isNullable());
		assertEquals("java.lang.Long", ((SimpleValue)property.getValue()).getTypeName(), "java.lang.Long because of primary key");
	}

	@Test
	public void testColumnPropertyNameMappings() {
		OverrideRepository or = new OverrideRepository();
		
		or.addResource(OVERRIDE_TEST_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(null);

		assertNull(repository.columnToPropertyName(TableIdentifier.create(null, null, "blah"), "bogus"));
		assertNull(repository.columnToPropertyName(TableIdentifier.create(null, null, "ORDERS"), "cust_id"));
		assertEquals("orderName", repository.columnToPropertyName(TableIdentifier.create(null, null, "ORDERS"), "NAME"));
	}
	
	@Test
	public void testMetaAttributeMappings() {
		PersistentClass classMapping = metadata.getEntityBinding( "Orders" );
		assertEquals("order table value", classMapping.getMetaAttribute( "order-meta" ).getValue());
		
		Property property = classMapping.getProperty("orderName");
		assertEquals("order column value", property.getMetaAttribute( "order-meta" ).getValue());
		//TODO: test sequence of meta
	}
	
	@Test
	public void testIdGenerator() {
		OverrideRepository or = new OverrideRepository();
		
		or.addResource(OVERRIDE_TEST_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(null);

		TableIdentifier miscTable = TableIdentifier.create(null,null, "MISC_TYPES");
		assertEquals("sequence",repository.getTableIdentifierStrategyName(miscTable));
		Map<?,?> props = repository.getTableIdentifierProperties(miscTable);
		assertEquals("seq_table", props.get("table"));
		
		assertNull(repository.getTableIdentifierStrategyName(TableIdentifier.create(null, null, "blah")));
		assertNull(repository.getTableIdentifierProperties(TableIdentifier.create(null, null, "blah")));
		TableIdentifier ordersTable = TableIdentifier.create(null,null, "ORDERS");
		
		assertEquals("customOrderId", repository.tableToIdentifierPropertyName(ordersTable));
        assertNull(repository.tableToIdentifierPropertyName(TableIdentifier.create(null, null, "blah")));
		
		assertEquals("CustomOID", repository.tableToCompositeIdName(ordersTable));
        assertNull(repository.tableToCompositeIdName(TableIdentifier.create(null, null, "blah")));
		
		List<String> primaryKeyColumnNames = repository.getPrimaryKeyColumnNames(TableIdentifier.create(null, null, "blah"));
		assertNull(primaryKeyColumnNames);
		
		primaryKeyColumnNames = repository.getPrimaryKeyColumnNames(ordersTable);
		assertNotNull(primaryKeyColumnNames);
		assertEquals(2, primaryKeyColumnNames.size());
		assertEquals("ORDERID", primaryKeyColumnNames.get(0));
		assertEquals("CUSTID", primaryKeyColumnNames.get(1));
		assertFalse(repository.excludeColumn(ordersTable, "CUSTID"));
		
		// applied
		PersistentClass classMapping = metadata.getEntityBinding("Orders");
		EnhancedValue ev = (EnhancedValue) classMapping.getIdentifier();
		assertEquals("CustomOID", ((Component)ev).getComponentClassName());
		
		assertEquals(2,classMapping.getIdentifierProperty().getColumnSpan());	
		
		Property identifierProperty = classMapping.getIdentifierProperty();
		assertEquals("customOrderId", identifierProperty.getName());
		
		classMapping = metadata.getEntityBinding("MiscTypes");
		ev = (EnhancedValue) classMapping.getIdentifier(); 
		assertEquals("sequence", ev.getIdentifierGeneratorStrategy()); // will fail if default schema is not set since then there is no match in the override binder		
		
		assertNotNull(ev.getIdentifierGeneratorProperties());
		assertEquals("seq_table", ev.getIdentifierGeneratorProperties().getProperty("table"));
		
	}
	
	@Test
	public void testReadExcludeTables() {
		OverrideRepository or = new OverrideRepository();
		
		or.addResource(OVERRIDE_TEST_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(null);
		
		assertTrue(repository.excludeTable(TableIdentifier.create(null,null, "DoNotWantIt") ) );
		assertFalse(repository.excludeTable(TableIdentifier.create(null,null, "NotListedThere") ) );
		assertFalse(repository.excludeTable(TableIdentifier.create("cat","sch", "WantedTable") ) );
		assertFalse(repository.excludeTable(TableIdentifier.create("BAD","SCHEMA", "WantedTable") ) ); 
		assertTrue(repository.excludeTable(TableIdentifier.create("BAD","SCHEMA", "SomethingElse") ) );
		
	}
	
	@Test
	public void testReadPackageName() {
		OverrideRepository or = new OverrideRepository();
		
		or.addResource(OVERRIDE_TEST_REVENG_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(new DefaultStrategy());
		
		assertEquals("org.werd.Q", repository.tableToClassName(TableIdentifier.create("q","Werd", "Q") ) );
		assertEquals("Notknown", repository.tableToClassName(TableIdentifier.create(null,null, "notknown") ) );
		
		assertEquals("org.werd.MyWorld", repository.tableToClassName(TableIdentifier.create(null,"Werd", "TBL_PKG") ) );
		assertEquals("other.MyWorld", repository.tableToClassName(TableIdentifier.create(null,"Werd", "TBL_OTHER") ) );
		
	}
	
	@Test
	public void testRevEngExclude() {
		
		assertNull(HibernateUtil.getTable(
				metadata, 
				JdbcUtil.toIdentifier(this, "DEFUNCT_TABLE") ) );
		Table foundTable = HibernateUtil.getTable(
				metadata, 
				JdbcUtil.toIdentifier(this, "INTHEMIDDLE") );
		assertNotNull(foundTable);
		Iterator<ForeignKey> fkiter = foundTable.getForeignKeyCollection().iterator();
		ForeignKey fk1 = fkiter.next();
		assertNotNull(fk1);
		assertFalse(fkiter.hasNext() );
		
		
	}
	
	@Test
	public void testSQLTypeMappingComparisons() {
		SQLTypeMapping one = new SQLTypeMapping(Types.BIGINT, 5, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE,SQLTypeMapping.UNKNOWN_NULLABLE);
		SQLTypeMapping two = new SQLTypeMapping(Types.BIGINT, SQLTypeMapping.UNKNOWN_LENGTH, 3, SQLTypeMapping.UNKNOWN_SCALE, SQLTypeMapping.UNKNOWN_NULLABLE);
		SQLTypeMapping generic = new SQLTypeMapping(Types.BIGINT, SQLTypeMapping.UNKNOWN_LENGTH, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, SQLTypeMapping.UNKNOWN_NULLABLE);
		SQLTypeMapping specific = new SQLTypeMapping(Types.BIGINT, 2, 3, 4, SQLTypeMapping.UNKNOWN_NULLABLE);
		SQLTypeMapping morespecific = new SQLTypeMapping(Types.BIGINT, 2, 3, 4, Boolean.TRUE);
		SQLTypeMapping equalmorespecific = new SQLTypeMapping(Types.BIGINT, 2, 3, 4, Boolean.TRUE);

        assertNotEquals(one, two);
        assertNotEquals(two, one);
        assertEquals(two, two);
        assertEquals(one, one);
        assertEquals(morespecific, equalmorespecific);
		
		
		assertEquals(-1, one.compareTo(two) );
		assertEquals(1, two.compareTo(one) );
		
		assertEquals(1, generic.compareTo(one) );
		assertEquals(1, generic.compareTo(two) );
		assertEquals(1, generic.compareTo(specific) );
		
		assertEquals(-1, specific.compareTo(one) );
		assertEquals(-1, specific.compareTo(two) );
		assertEquals(-1, specific.compareTo(generic) );
		assertEquals(1, specific.compareTo(morespecific) );
		assertEquals(-1, morespecific.compareTo(specific) );
		
	}
	
	@Test
	public void testSqlTypeOverride() {
		
		OverrideRepository or = new OverrideRepository();
		
		SQLTypeMapping sqltype = new SQLTypeMapping(Types.BINARY);
		
		sqltype.setLength(1);
		sqltype.setHibernateType("boolean");
		or.addTypeMapping(sqltype);
		
		sqltype = new SQLTypeMapping(Types.BIT);
		
		sqltype.setHibernateType("yes_no");
		or.addTypeMapping(sqltype);
		
		RevengStrategy res = or.getReverseEngineeringStrategy(null);
		assertEquals("boolean",res.columnToHibernateTypeName(null,null, Types.BINARY, 1, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
        assertNull(res.columnToHibernateTypeName(null, null, Types.LONGVARCHAR, 1, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, false, false));
		assertEquals("yes_no",res.columnToHibernateTypeName(null,null, Types.BIT, SQLTypeMapping.UNKNOWN_LENGTH, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, false, false) );
	}
	
	@Test
	public void testTableExclude() {
		TableFilter tf = new TableFilter();
		tf.setMatchName("max");
		tf.setExclude(Boolean.TRUE);
		assertTrue(tf.exclude(TableIdentifier.create(null, null, "max")));
		assertNull(tf.exclude(TableIdentifier.create(null, null, "maxnotexact") ) );
		tf.setMatchName(".*max");
		assertTrue(tf.exclude(TableIdentifier.create(null, null, "max")));
		assertNull(tf.exclude(TableIdentifier.create(null, null, "maxnotending") ) );
		assertTrue(tf.exclude(TableIdentifier.create(null, null, "endingWithmax")));
		tf.setMatchName("max.*");
		assertTrue(tf.exclude(TableIdentifier.create(null, null, "max")));
		tf.setMatchName(".*max.*");
		assertTrue(tf.exclude(TableIdentifier.create(null, null, "max")));
		assertNull(tf.exclude(TableIdentifier.create(null, null, "notxam") ) );
		assertTrue(tf.exclude(TableIdentifier.create(null, null, "heremaxsub")));
	}
	
	@Test
	public void testColumnExclude() {
		
		OverrideRepository or = new OverrideRepository();
		or.addResource(OVERRIDE_TEST_REVENG_XML);
		
		RevengStrategy reverseEngineeringStrategy = or.getReverseEngineeringStrategy(null);
		
		assertFalse(reverseEngineeringStrategy.excludeColumn(TableIdentifier.create(null, null, "EXCOLUMNS"), "blah"));
		assertFalse(reverseEngineeringStrategy.excludeColumn(TableIdentifier.create(null, null, "EXCOLUMNS"), "NAME"));
		assertTrue(reverseEngineeringStrategy.excludeColumn(TableIdentifier.create(null, null, "EXCOLUMNS"), "EXCOLUMN"));
		
		Table table = HibernateUtil.getTable(metadata, JdbcUtil.toIdentifier(this, "EXCOLUMNS"));
		assertNotNull(table);
		
		assertNotNull(table.getColumn(new Column("name")));
		assertNull(table.getColumn(new Column("excolumn")));
		
	}
	
	@Test
	public void testSimpleUserDefinedForeignKeys() {
		
		Table table = HibernateUtil.getTable(metadata, JdbcUtil.toIdentifier(this, "ORDERS") );
		
		Iterator<ForeignKey> foreignKeyIterator = table.getForeignKeyCollection().iterator();
		ForeignKey fk = foreignKeyIterator.next();
		assertEquals(fk.getReferencedTable().getName(), JdbcUtil.toIdentifier(this, "CUSTOMER") );
		
		PersistentClass classMapping = metadata.getEntityBinding("Orders");
		classMapping.getProperty("customer");
		
		classMapping = metadata.getEntityBinding("Customer");
		classMapping.getProperty("orderses");
			
	}
	
	@Test
	public void testCompositeUserDefinedForeignKeys() {
		
		Table table = HibernateUtil.getTable(metadata, JdbcUtil.toIdentifier(this, "CHILDREN") );
		
		Iterator<ForeignKey> foreignKeyIterator = table.getForeignKeyCollection().iterator();
		ForeignKey fk = foreignKeyIterator.next();
		assertEquals(fk.getReferencedTable().getName(), JdbcUtil.toIdentifier(this, "PARENT") );
		assertEquals(2, fk.getReferencedColumns().size());
		assertEquals("child_to_parent", fk.getName());
		
		PersistentClass classMapping = metadata.getEntityBinding("Children");
		Property property = classMapping.getProperty("propertyParent");
		assertEquals(2,property.getColumnSpan());
		
		classMapping = metadata.getEntityBinding("Parent");
		classMapping.getProperty("propertyChildren");
			
	}
		
	@Test
	public void testTypes() {		
		PersistentClass classMapping = metadata.getEntityBinding("MiscTypes");
		assertEquals(
				"org.hibernate.tool.jdbc2cfg.OverrideBinder.SomeUserType", 
				((SimpleValue)classMapping.getProperty("name").getValue()).getTypeName());
		assertEquals(
				"string", 
				((SimpleValue)classMapping.getProperty("shortname").getValue()).getTypeName());
		assertEquals(
				"yes_no", 
				((SimpleValue)classMapping.getProperty("flag").getValue()).getTypeName());		
	}
	
	@Test
	public void testTableToClass() {
		
		RevengStrategy res = new OverrideRepository().addResource(OVERRIDE_TEST_REVENG_XML).getReverseEngineeringStrategy(new DefaultStrategy());
		
		TableIdentifier tableIdentifier = TableIdentifier.create(null, null, "TblTest");
		assertEquals("org.test.Test", res.tableToClassName(tableIdentifier));		
		
		tableIdentifier = TableIdentifier.create(
				Environment
					.getProperties()
					.getProperty(AvailableSettings.DEFAULT_CATALOG), 
				"Werd", 
				"Testy");
		assertEquals("org.werd.Testy", res.tableToClassName(tableIdentifier));
		
		tableIdentifier = TableIdentifier.create(null, null, "Nothing");
		assertEquals("Nothing", res.tableToClassName(tableIdentifier));
		
	}
	
	@Test
	public void testMetaAttributes() {
		
		RevengStrategy res = new OverrideRepository().addResource(OVERRIDE_TEST_REVENG_XML).getReverseEngineeringStrategy(new DefaultStrategy());
		
		TableIdentifier tableIdentifier = TableIdentifier.create(null, null, "TblTest");
		Map<String,MetaAttribute> attributes = res.tableToMetaAttributes(tableIdentifier);
		assertNotNull(attributes);
		assertEquals(1, attributes.size());
		MetaAttribute ma = attributes.get("use-in-test");
		assertEquals("use-in-test", ma.getName());
		assertEquals("true", ma.getValue());
				
		tableIdentifier = TableIdentifier.create(
				Environment
					.getProperties()
					.getProperty(AvailableSettings.DEFAULT_CATALOG), 
				"Werd", 
				"Testy");
		attributes = res.tableToMetaAttributes( tableIdentifier );
		assertNotNull(attributes);
		ma = attributes.get( "werd-meta" );
		assertEquals("werd-meta", ma.getName());
		assertEquals(2, ma.getValues().size());
	
		tableIdentifier = TableIdentifier.create(null, "Werd", "MetaTable");
		attributes = res.tableToMetaAttributes( tableIdentifier );
		assertNotNull(attributes);
		assertEquals(2, attributes.size());
		ma = attributes.get("specific-werd");
		assertEquals("specific-werd", ma.getName());
		assertEquals("a one", ma.getValue());
		
		ma = attributes.get( "werd-meta" );
		assertEquals("werd-meta", ma.getName());
		assertEquals(1, ma.getValues().size()); // as long as no inherit this should be one
		assertEquals("value three", ma.getValue());
	
		tableIdentifier = TableIdentifier.create(null, null, "Nothing");
        assertNull(res.tableToMetaAttributes(tableIdentifier));
		
		assertNull(res.columnToMetaAttributes(TableIdentifier.create(null, null, "Nothing"), "bogus"));
		assertNull(res.columnToMetaAttributes( TableIdentifier.create(null, "Werd", "MetaTable"), "bogusColumn" ));
		attributes = res.columnToMetaAttributes( TableIdentifier.create(null, "Werd", "MetaTable"), "MetaColumn" );
		assertEquals(1, attributes.size());
		ma = attributes.get("specific-column");
		assertEquals("specific-column",ma.getName());
		assertEquals("yes a column with meta",ma.getValue());
		
	}
	
	private SchemaSelection createDummySchemaSelection() {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return null;
			}
			@Override
			public String getMatchSchema() {
				return null;
			}
			@Override
			public String getMatchTable() {
				return "DUMMY.*";
			}
		};
	}

}
