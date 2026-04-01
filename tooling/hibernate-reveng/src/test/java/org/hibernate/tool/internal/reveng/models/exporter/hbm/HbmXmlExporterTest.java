/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link HbmXmlExporter}.
 *
 * @author Koen Aers
 */
public class HbmXmlExporterTest {

	private String export(TableMetadata table) {
		return export(table, null, Collections.emptyMap());
	}

	private String export(TableMetadata table, String comment,
						   Map<String, List<String>> metaAttributes) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, comment, metaAttributes);
		return writer.toString();
	}

	@Test
	public void testXmlHeader() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<?xml version=\"1.0\"?>"), xml);
		assertTrue(xml.contains("<!DOCTYPE hibernate-mapping"), xml);
		assertTrue(xml.contains("<hibernate-mapping"), xml);
		assertTrue(xml.contains("</hibernate-mapping>"), xml);
	}

	@Test
	public void testClassElement() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<class"), xml);
		assertTrue(xml.contains("name=\"com.example.Employee\""), xml);
		assertTrue(xml.contains("table=\"EMPLOYEE\""), xml);
		assertTrue(xml.contains("</class>"), xml);
	}

	@Test
	public void testClassWithSchemaAndCatalog() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.setCatalog("MYDB");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("schema=\"HR\""), xml);
		assertTrue(xml.contains("catalog=\"MYDB\""), xml);
	}

	@Test
	public void testTableComment() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table, "Employee table", Collections.emptyMap());
		assertTrue(xml.contains("<comment>Employee table</comment>"), xml);
	}

	@Test
	public void testIdWithGenerator() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		String xml = export(table);
		assertTrue(xml.contains("<id"), xml);
		assertTrue(xml.contains("name=\"id\""), xml);
		assertTrue(xml.contains("<column name=\"ID\""), xml);
		assertTrue(xml.contains("<generator class=\"identity\"/>"), xml);
		assertTrue(xml.contains("</id>"), xml);
	}

	@Test
	public void testIdWithSequenceGenerator() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.SEQUENCE));
		String xml = export(table);
		assertTrue(xml.contains("<generator class=\"sequence\"/>"), xml);
	}

	@Test
	public void testIdWithAssignedGenerator() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<generator class=\"assigned\"/>"), xml);
	}

	@Test
	public void testIdTypeName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("type=\"java.lang.Long\""), xml);
	}

	@Test
	public void testBasicProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String xml = export(table);
		assertTrue(xml.contains("<property"), xml);
		assertTrue(xml.contains("name=\"name\""), xml);
		assertTrue(xml.contains("type=\"string\""), xml);
		assertTrue(xml.contains("<column name=\"NAME\""), xml);
		assertTrue(xml.contains("</property>"), xml);
	}

	@Test
	public void testPropertyColumnAttributes() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		String xml = export(table);
		assertTrue(xml.contains("not-null=\"true\""), xml);
		assertTrue(xml.contains("unique=\"true\""), xml);
		assertTrue(xml.contains("length=\"100\""), xml);
		assertTrue(xml.contains("precision=\"10\""), xml);
		assertTrue(xml.contains("scale=\"2\""), xml);
	}

	@Test
	public void testVersionProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		String xml = export(table);
		assertTrue(xml.contains("<version"), xml);
		assertTrue(xml.contains("name=\"version\""), xml);
		assertTrue(xml.contains("type=\"java.lang.Integer\""), xml);
		assertTrue(xml.contains("<column name=\"VERSION\""), xml);
		assertTrue(xml.contains("</version>"), xml);
		assertFalse(xml.contains("<property\n        name=\"version\""), xml);
	}

	@Test
	public void testManyToOne() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<many-to-one"), xml);
		assertTrue(xml.contains("name=\"department\""), xml);
		assertTrue(xml.contains("class=\"com.example.Department\""), xml);
		assertTrue(xml.contains("<column name=\"DEPT_ID\"/>"), xml);
		assertTrue(xml.contains("</many-to-one>"), xml);
		assertFalse(xml.contains("name=\"deptId\""), xml);
	}

	@Test
	public void testManyToOneNotNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.optional(false));
		String xml = export(table);
		assertTrue(xml.contains("not-null=\"true\""), xml);
	}

	@Test
	public void testManyToOneLazy() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY));
		String xml = export(table);
		assertTrue(xml.contains("fetch=\"select\""), xml);
		assertTrue(xml.contains("lazy=\"proxy\""), xml);
	}

	@Test
	public void testManyToOneDefaultFetch() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String xml = export(table);
		assertFalse(xml.contains("fetch="), xml);
		assertFalse(xml.contains("lazy="), xml);
	}

	@Test
	public void testOneToOneOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		String xml = export(table);
		assertTrue(xml.contains("<one-to-one"), xml);
		assertTrue(xml.contains("name=\"address\""), xml);
		assertTrue(xml.contains("class=\"com.example.Address\""), xml);
		assertTrue(xml.contains("constrained=\"true\""), xml);
	}

	@Test
	public void testOneToOneInverse() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address"));
		String xml = export(table);
		assertTrue(xml.contains("property-ref=\"address\""), xml);
		assertFalse(xml.contains("constrained"), xml);
	}

	@Test
	public void testOneToOneWithCascade() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.cascade(CascadeType.ALL));
		String xml = export(table);
		assertTrue(xml.contains("cascade=\"all\""), xml);
	}

	@Test
	public void testOneToManySet() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<set name=\"employees\""), xml);
		assertTrue(xml.contains("inverse=\"true\""), xml);
		assertTrue(xml.contains("<key>"), xml);
		assertTrue(xml.contains("<column name=\"department\"/>"), xml);
		assertTrue(xml.contains("<one-to-many class=\"com.example.Employee\"/>"), xml);
		assertTrue(xml.contains("</set>"), xml);
	}

	@Test
	public void testOneToManyWithCascade() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.cascade(CascadeType.ALL));
		String xml = export(table);
		assertTrue(xml.contains("cascade=\"all\""), xml);
	}

	@Test
	public void testOneToManyEager() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER));
		String xml = export(table);
		assertTrue(xml.contains("lazy=\"false\""), xml);
	}

	@Test
	public void testManyToManyOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		String xml = export(table);
		assertTrue(xml.contains("<set name=\"projects\""), xml);
		assertTrue(xml.contains("table=\"EMPLOYEE_PROJECT\""), xml);
		assertTrue(xml.contains("<column name=\"EMPLOYEE_ID\"/>"), xml);
		assertTrue(xml.contains("<many-to-many class=\"com.example.Project\">"), xml);
		assertTrue(xml.contains("<column name=\"PROJECT_ID\"/>"), xml);
		assertTrue(xml.contains("</many-to-many>"), xml);
		assertTrue(xml.contains("</set>"), xml);
	}

	@Test
	public void testManyToManyInverse() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects"));
		String xml = export(table);
		assertTrue(xml.contains("<set name=\"employees\""), xml);
		assertTrue(xml.contains("inverse=\"true\""), xml);
		assertFalse(xml.contains("<set name=\"employees\"\n        table="), xml);
	}

	@Test
	public void testManyToManyWithCascade() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE));
		String xml = export(table);
		assertTrue(xml.contains("cascade=\"persist, merge\""), xml);
	}

	@Test
	public void testCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		String xml = export(table);
		assertTrue(xml.contains("<composite-id"), xml);
		assertTrue(xml.contains("name=\"id\""), xml);
		assertTrue(xml.contains("class=\"com.example.OrderLineId\""), xml);
		assertTrue(xml.contains("<key-property name=\"orderId\">"), xml);
		assertTrue(xml.contains("<column name=\"ORDER_ID\"/>"), xml);
		assertTrue(xml.contains("<key-property name=\"lineNumber\">"), xml);
		assertTrue(xml.contains("<column name=\"LINE_NUMBER\"/>"), xml);
		assertTrue(xml.contains("</composite-id>"), xml);
		assertFalse(xml.contains("<id "), xml);
	}

	@Test
	public void testEmbeddedComponent() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET")
				.addAttributeOverride("city", "HOME_CITY"));
		String xml = export(table);
		assertTrue(xml.contains("<component name=\"homeAddress\" class=\"com.example.Address\">"), xml);
		assertTrue(xml.contains("<property name=\"street\">"), xml);
		assertTrue(xml.contains("<column name=\"HOME_STREET\"/>"), xml);
		assertTrue(xml.contains("<property name=\"city\">"), xml);
		assertTrue(xml.contains("<column name=\"HOME_CITY\"/>"), xml);
		assertTrue(xml.contains("</component>"), xml);
	}

	@Test
	public void testInheritanceRootWithDiscriminator() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER)
				.discriminatorColumnLength(10));
		String xml = export(table);
		assertTrue(xml.contains("<class"), xml);
		assertTrue(xml.contains("<discriminator"), xml);
		assertTrue(xml.contains("column=\"DTYPE\""), xml);
		assertTrue(xml.contains("type=\"integer\""), xml);
		assertTrue(xml.contains("length=\"10\""), xml);
	}

	@Test
	public void testInheritanceSingleTableSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.discriminatorValue("CAR");
		String xml = export(table);
		assertTrue(xml.contains("<subclass"), xml);
		assertTrue(xml.contains("name=\"com.example.Car\""), xml);
		assertTrue(xml.contains("extends=\"com.example.Vehicle\""), xml);
		assertTrue(xml.contains("discriminator-value=\"CAR\""), xml);
		assertTrue(xml.contains("</subclass>"), xml);
		assertFalse(xml.contains("table="), xml);
	}

	@Test
	public void testInheritanceJoinedSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		String xml = export(table);
		assertTrue(xml.contains("<joined-subclass"), xml);
		assertTrue(xml.contains("extends=\"com.example.Vehicle\""), xml);
		assertTrue(xml.contains("<key>"), xml);
		assertTrue(xml.contains("<column name=\"VEHICLE_ID\"/>"), xml);
		assertTrue(xml.contains("</key>"), xml);
		assertTrue(xml.contains("</joined-subclass>"), xml);
	}

	@Test
	public void testMetaAttributes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		Map<String, List<String>> metaAttributes = Map.of(
				"class-description", List.of("Employee entity"));
		String xml = export(table, null, metaAttributes);
		assertTrue(xml.contains("<meta attribute=\"class-description\">Employee entity</meta>"), xml);
	}

	@Test
	public void testForeignKeyColumnSkippedInProperties() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String xml = export(table);
		assertFalse(xml.contains("<property\n        name=\"deptId\""), xml);
		assertTrue(xml.contains("<many-to-one"), xml);
	}

	@Test
	public void testCustomTemplatePath(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("main.hbm.ftl"),
				"<!-- Custom HBM for ${helper.getClassName()} -->");
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new String[] { tempDir.toString() });
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		assertEquals("<!-- Custom HBM for com.example.Employee -->", writer.toString());
	}

	@Test
	public void testNoPackageEntity() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("name=\"Employee\""), xml);
	}

	@Test
	public void testNamedQueryWithAttributes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		NamedQueryAnnotation nq = HibernateAnnotations.NAMED_QUERY.createUsage(ctx);
		nq.name("findActive");
		nq.query("SELECT e FROM Employee e WHERE e.active = true");
		nq.flushMode(org.hibernate.annotations.FlushModeType.AUTO);
		nq.cacheable(true);
		nq.cacheRegion("empRegion");
		nq.fetchSize(20);
		nq.timeout(1000);
		((DynamicClassDetails) entity).addAnnotationUsage(nq);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("name=\"findActive\""), xml);
		assertTrue(xml.contains("flush-mode=\"auto\""), xml);
		assertTrue(xml.contains("cacheable=\"true\""), xml);
		assertTrue(xml.contains("cache-region=\"empRegion\""), xml);
		assertTrue(xml.contains("fetch-size=\"20\""), xml);
		assertTrue(xml.contains("timeout=\"1000\""), xml);
	}

	@Test
	public void testNamedNativeQueryWithQuerySpaces() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findNative");
		nnq.query("SELECT * FROM EMPLOYEE WHERE ACTIVE = 1");
		nnq.cacheable(true);
		nnq.querySpaces(new String[]{"EMPLOYEE", "DEPARTMENT"});
		((DynamicClassDetails) entity).addAnnotationUsage(nnq);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("name=\"findNative\""), xml);
		assertTrue(xml.contains("cacheable=\"true\""), xml);
		assertTrue(xml.contains("<synchronize table=\"EMPLOYEE\"/>"), xml);
		assertTrue(xml.contains("<synchronize table=\"DEPARTMENT\"/>"), xml);
	}
}
