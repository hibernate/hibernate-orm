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
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
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
	public void testGeneratedHeader() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.matches("(?s).*<!-- Generated .+ by Hibernate Tools .+ -->.*"), xml);
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
	public void testManyToOnePropertyRef() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_CODE", "deptCode", String.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE"));
		String xml = export(table);
		assertTrue(xml.contains("property-ref=\"CODE\""), xml);
	}

	@Test
	public void testManyToOneNoPropertyRef() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String xml = export(table);
		assertFalse(xml.contains("property-ref"), xml);
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
	public void testCompositeIdWithKeyManyToOne() {
		TableMetadata table = new TableMetadata("CUSTOMER_ORDER", "CustomerOrder", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "CustomerOrderId", "com.example")
				.addAttributeOverride("orderNumber", "ORDER_NUMBER")
				.addKeyManyToOne("customer", "CUSTOMER_ID", "Customer", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<composite-id"), xml);
		assertTrue(xml.contains("name=\"id\""), xml);
		assertTrue(xml.contains("class=\"com.example.CustomerOrderId\""), xml);
		assertTrue(xml.contains("<key-property name=\"orderNumber\">"), xml);
		assertTrue(xml.contains("<column name=\"ORDER_NUMBER\"/>"), xml);
		assertTrue(xml.contains("<key-many-to-one name=\"customer\" class=\"com.example.Customer\">"), xml);
		assertTrue(xml.contains("<column name=\"CUSTOMER_ID\"/>"), xml);
		assertTrue(xml.contains("</key-many-to-one>"), xml);
		assertTrue(xml.contains("</composite-id>"), xml);
	}

	@Test
	public void testCompositeIdWithMultipleKeyManyToOnes() {
		TableMetadata table = new TableMetadata("ENROLLMENT", "Enrollment", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "EnrollmentId", "com.example")
				.addKeyManyToOne("student", "STUDENT_ID", "Student", "com.example")
				.addKeyManyToOne("course", "COURSE_ID", "Course", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<key-many-to-one name=\"student\" class=\"com.example.Student\">"), xml);
		assertTrue(xml.contains("<column name=\"STUDENT_ID\"/>"), xml);
		assertTrue(xml.contains("<key-many-to-one name=\"course\" class=\"com.example.Course\">"), xml);
		assertTrue(xml.contains("<column name=\"COURSE_ID\"/>"), xml);
		assertFalse(xml.contains("<key-property"), xml);
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

	@Test
	public void testNamedNativeQueryWithResultClass() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findAll");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultClass(String.class);
		((DynamicClassDetails) entity).addAnnotationUsage(nnq);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<return alias=\"java.lang.String\" class=\"java.lang.String\"/>"), xml);
	}

	@Test
	public void testNamedNativeQueryWithResultSetMapping() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Create @SqlResultSetMapping
		SqlResultSetMappingJpaAnnotation mapping = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(ctx);
		mapping.name("empMapping");
		EntityResultJpaAnnotation er = JpaAnnotations.ENTITY_RESULT.createUsage(ctx);
		er.entityClass(String.class);
		FieldResultJpaAnnotation fr = JpaAnnotations.FIELD_RESULT.createUsage(ctx);
		fr.name("id");
		fr.column("EMP_ID");
		er.fields(new jakarta.persistence.FieldResult[]{fr});
		mapping.entities(new jakarta.persistence.EntityResult[]{er});
		ColumnResultJpaAnnotation cr = JpaAnnotations.COLUMN_RESULT.createUsage(ctx);
		cr.name("EXTRA_COL");
		mapping.columns(new jakarta.persistence.ColumnResult[]{cr});
		((DynamicClassDetails) entity).addAnnotationUsage(mapping);
		// Create @NamedNativeQuery referencing the mapping
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findWithMapping");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultSetMapping("empMapping");
		((DynamicClassDetails) entity).addAnnotationUsage(nnq);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<return alias=\"java.lang.String\" class=\"java.lang.String\">"), xml);
		assertTrue(xml.contains("<return-property name=\"id\" column=\"EMP_ID\"/>"), xml);
		assertTrue(xml.contains("</return>"), xml);
		assertTrue(xml.contains("<return-scalar column=\"EXTRA_COL\"/>"), xml);
	}

	// --- Import elements ---

	@Test
	public void testImportElements() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		Map<String, String> imports = new java.util.LinkedHashMap<>();
		imports.put("com.example.Employee", "Emp");
		imports.put("com.example.Department", "Dept");
		exporter.export(writer, entity, null, Collections.emptyMap(), imports);
		String xml = writer.toString();
		assertTrue(xml.contains("<import class=\"com.example.Employee\" rename=\"Emp\"/>"), xml);
		assertTrue(xml.contains("<import class=\"com.example.Department\" rename=\"Dept\"/>"), xml);
	}

	@Test
	public void testImportElementsExcludesSameName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		Map<String, String> imports = Map.of("com.example.Employee", "com.example.Employee");
		exporter.export(writer, entity, null, Collections.emptyMap(), imports);
		String xml = writer.toString();
		assertFalse(xml.contains("<import"), xml);
	}

	@Test
	public void testNoImportElements() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, new DynamicEntityBuilder().createEntityFromTable(table));
		String xml = writer.toString();
		assertFalse(xml.contains("<import"), xml);
	}

	// --- Field meta-attributes ---

	@Test
	public void testFieldMetaAttributeOnProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"name", Map.of("default-value", List.of("N/A")));
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, null, Collections.emptyMap(),
				Collections.emptyMap(), fieldMeta);
		String xml = writer.toString();
		assertTrue(xml.contains("<meta attribute=\"default-value\">N/A</meta>"), xml);
	}

	@Test
	public void testFieldMetaAttributeOnCollection() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata("employees", "department", "Employee", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"employees", Map.of("property-type", List.of("java.util.List")));
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, null, Collections.emptyMap(),
				Collections.emptyMap(), fieldMeta);
		String xml = writer.toString();
		assertTrue(xml.contains("<meta attribute=\"property-type\">java.util.List</meta>"), xml);
	}

	@Test
	public void testFieldMetaAttributeMultipleValues() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"name", Map.of("scope-field", List.of("protected"),
						"use-in-tostring", List.of("true")));
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, null, Collections.emptyMap(),
				Collections.emptyMap(), fieldMeta);
		String xml = writer.toString();
		assertTrue(xml.contains("<meta attribute=\"scope-field\">protected</meta>"), xml);
		assertTrue(xml.contains("<meta attribute=\"use-in-tostring\">true</meta>"), xml);
	}

	@Test
	public void testNoFieldMetaAttributes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, new DynamicEntityBuilder().createEntityFromTable(table));
		String xml = writer.toString();
		// Should have no <meta> at all (no class-level or field-level)
		assertFalse(xml.contains("<meta attribute="), xml);
	}

	// --- Collection cache ---

	@Test
	public void testCollectionCacheOnOneToMany() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata("employees", "department", "Employee", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Add @Cache to the collection field
		for (var field : entity.getFields()) {
			if ("employees".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.CacheAnnotation cache =
						HibernateAnnotations.CACHE.createUsage(ctx);
				cache.usage(org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE);
				((DynamicClassDetails) entity).getFields().stream()
						.filter(f -> f.getName().equals("employees"))
						.findFirst()
						.ifPresent(f -> ((org.hibernate.models.internal.dynamic.DynamicFieldDetails) f)
								.addAnnotationUsage(cache));
				break;
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<cache usage=\"read-write\"/>"), xml);
	}

	@Test
	public void testCollectionCacheWithRegion() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata("employees", "department", "Employee", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("employees".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.CacheAnnotation cache =
						HibernateAnnotations.CACHE.createUsage(ctx);
				cache.usage(org.hibernate.annotations.CacheConcurrencyStrategy.READ_ONLY);
				cache.region("emp-cache");
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(cache);
				break;
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<cache usage=\"read-only\" region=\"emp-cache\"/>"), xml);
	}

	@Test
	public void testNoCollectionCache() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata("employees", "department", "Employee", "com.example"));
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, new DynamicEntityBuilder().createEntityFromTable(table));
		String xml = writer.toString();
		// The class-level cache check — collection has no cache
		assertFalse(xml.contains("<cache usage="), xml);
	}

	// --- Collection SQL operations ---

	@Test
	public void testCollectionSQLDeleteAll() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata("employees", "department", "Employee", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("employees".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation sda =
						HibernateAnnotations.SQL_DELETE_ALL.createUsage(ctx);
				sda.sql("DELETE FROM EMP WHERE dept_id = ?");
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(sda);
				break;
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<sql-delete-all>DELETE FROM EMP WHERE dept_id = ?</sql-delete-all>"), xml);
	}

	@Test
	public void testCollectionSQLDeleteAllCallable() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata("employees", "department", "Employee", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("employees".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation sda =
						HibernateAnnotations.SQL_DELETE_ALL.createUsage(ctx);
				sda.sql("{call deleteAllEmps(?)}");
				sda.callable(true);
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(sda);
				break;
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<sql-delete-all callable=\"true\">{call deleteAllEmps(?)}</sql-delete-all>"), xml);
	}

	// --- Composite join columns ---

	@Test
	public void testCompositeJoinColumnsOnManyToOne() {
		TableMetadata table = new TableMetadata("ORDER_ITEM", "OrderItem", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addForeignKey(new ForeignKeyMetadata(
				"order", "ORDER_ID", "Order", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Replace the single @JoinColumn with @JoinColumns containing two columns
		for (var field : entity.getFields()) {
			if ("order".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				var jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc1.name("ORDER_ID");
				var jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc2.name("ORDER_SEQ");
				var jcs = JpaAnnotations.JOIN_COLUMNS.createUsage(ctx);
				jcs.value(new jakarta.persistence.JoinColumn[]{jc1, jc2});
				df.addAnnotationUsage(jcs);
				break;
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<column name=\"ORDER_ID\"/>"), xml);
		assertTrue(xml.contains("<column name=\"ORDER_SEQ\"/>"), xml);
	}

	// --- HibernateMappingSettings tests ---

	@Test
	public void testDefaultSettings() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertFalse(xml.contains("default-access"), xml);
		assertFalse(xml.contains("default-cascade"), xml);
		assertFalse(xml.contains("default-lazy"), xml);
		assertFalse(xml.contains("auto-import"), xml);
	}

	@Test
	public void testDefaultAccessField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("field", "none", true, true));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("default-access=\"field\""), xml);
	}

	@Test
	public void testDefaultAccessPropertyNotRendered() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("property", "none", true, true));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertFalse(xml.contains("default-access"), xml);
	}

	@Test
	public void testDefaultCascadeAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("property", "all", true, true));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("default-cascade=\"all\""), xml);
	}

	@Test
	public void testDefaultCascadeNoneNotRendered() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("property", "none", true, true));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertFalse(xml.contains("default-cascade"), xml);
	}

	@Test
	public void testDefaultLazyFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("property", "none", false, true));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("default-lazy=\"false\""), xml);
	}

	@Test
	public void testDefaultLazyTrueNotRendered() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("property", "none", true, true));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertFalse(xml.contains("default-lazy"), xml);
	}

	@Test
	public void testAutoImportFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("property", "none", true, false));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("auto-import=\"false\""), xml);
	}

	@Test
	public void testAutoImportTrueNotRendered() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("property", "none", true, true));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertFalse(xml.contains("auto-import"), xml);
	}

	@Test
	public void testAllNonDefaultSettings() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		HbmXmlExporter exporter = HbmXmlExporter.create(
				new HibernateMappingSettings("field", "save-update", false, false));
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("default-access=\"field\""), xml);
		assertTrue(xml.contains("default-cascade=\"save-update\""), xml);
		assertTrue(xml.contains("default-lazy=\"false\""), xml);
		assertTrue(xml.contains("auto-import=\"false\""), xml);
	}

	// --- Custom type <param> elements ---

	@Test
	public void testCustomTypeWithParams() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("SALARY", "salary", java.math.BigDecimal.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("salary".equals(field.getName())) {
				var typeAnn = org.hibernate.boot.models.HibernateAnnotations.TYPE.createUsage(ctx);
				typeAnn.value((Class) org.hibernate.usertype.UserType.class);
				var param = org.hibernate.boot.models.HibernateAnnotations.PARAMETER.createUsage(ctx);
				param.name("currency");
				param.value("USD");
				typeAnn.parameters(new org.hibernate.annotations.Parameter[]{param});
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(typeAnn);
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<type name=\"org.hibernate.usertype.UserType\">"), xml);
		assertTrue(xml.contains("<param name=\"currency\">USD</param>"), xml);
		assertFalse(xml.contains("type=\"org.hibernate.usertype.UserType\""), xml);
	}

	// --- composite-id mapped="true" (@IdClass) ---

	@Test
	public void testCompositeIdMapped() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("QUANTITY", "quantity", Integer.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Add @IdClass
		var idClassAnn = org.hibernate.boot.models.JpaAnnotations.ID_CLASS.createUsage(ctx);
		idClassAnn.value(java.io.Serializable.class);
		((DynamicClassDetails) entity).addAnnotationUsage(idClassAnn);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("mapped=\"true\""), xml);
		assertTrue(xml.contains("class=\"java.io.Serializable\""), xml);
		assertTrue(xml.contains("<key-property"), xml);
		assertTrue(xml.contains("name=\"orderId\""), xml);
		assertTrue(xml.contains("name=\"lineNumber\""), xml);
		assertFalse(xml.contains("<id"), xml);
	}

	// --- <map-key-many-to-many> ---

	@Test
	public void testMapKeyManyToMany() {
		TableMetadata table = new TableMetadata("ORDERS", "Order", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Manually add a Map<Product, OrderItem> field with @OneToMany + @MapKeyJoinColumn
		DynamicClassDetails keyEntity = new DynamicClassDetails(
				"Product", "com.example.Product", false, null, null, ctx);
		DynamicClassDetails valueEntity = new DynamicClassDetails(
				"OrderItem", "com.example.OrderItem", false, null, null, ctx);
		ClassDetails mapClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(java.util.Map.class.getName());
		org.hibernate.models.spi.TypeDetails keyType =
				new org.hibernate.models.internal.ClassTypeDetailsImpl(
						keyEntity, org.hibernate.models.spi.TypeDetails.Kind.CLASS);
		org.hibernate.models.spi.TypeDetails valueType =
				new org.hibernate.models.internal.ClassTypeDetailsImpl(
						valueEntity, org.hibernate.models.spi.TypeDetails.Kind.CLASS);
		org.hibernate.models.spi.TypeDetails fieldType =
				new org.hibernate.models.internal.ParameterizedTypeDetailsImpl(
						mapClass, java.util.List.of(keyType, valueType), null);
		var field = ((DynamicClassDetails) entity).applyAttribute(
				"items", fieldType, false, true, ctx);
		var o2m = org.hibernate.boot.models.JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.mappedBy("order");
		field.addAnnotationUsage(o2m);
		var mkjc = org.hibernate.boot.models.JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage(ctx);
		mkjc.name("PRODUCT_ID");
		field.addAnnotationUsage(mkjc);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<map-key-many-to-many class=\"com.example.Product\" column=\"PRODUCT_ID\"/>"), xml);
		assertFalse(xml.contains("<map-key column="), xml);
	}

	@Test
	public void testColumnComment() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DESCRIPTION", "description", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Add @Comment to the "description" field
		var descField = (DynamicClassDetails) entity;
		for (var field : entity.getFields()) {
			if ("description".equals(field.getName())) {
				var commentAnn = HibernateAnnotations.COMMENT.createUsage(ctx);
				commentAnn.value("Product description text");
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(commentAnn);
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<comment>Product description text</comment>"), xml);
		// The column with comment should not be self-closing
		assertTrue(xml.contains("<column name=\"DESCRIPTION\""), xml);
	}

	// --- optimistic-lock on many-to-one ---

	@Test
	public void testManyToOneOptimisticLockExcluded() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("department".equals(field.getName())) {
				var olAnn = HibernateAnnotations.OPTIMISTIC_LOCK.createUsage(ctx);
				olAnn.excluded(true);
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(olAnn);
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("optimistic-lock=\"false\""), xml);
	}

	// --- update/insert on many-to-one ---

	@Test
	public void testManyToOneUpdateInsertFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("department".equals(field.getName())) {
				// Replace the default @JoinColumn with one that has insertable=false, updatable=false
				var jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc.name("DEPT_ID");
				jc.updatable(false);
				jc.insertable(false);
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(jc);
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("update=\"false\""), xml);
		assertTrue(xml.contains("insert=\"false\""), xml);
	}

	// --- access on collections ---

	@Test
	public void testOneToManyAccessProperty() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "Employee", "com.example", "department"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("employees".equals(field.getName())) {
				var accessAnn = JpaAnnotations.ACCESS.createUsage(ctx);
				accessAnn.value(jakarta.persistence.AccessType.PROPERTY);
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(accessAnn);
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("access=\"property\""), xml);
	}

	// --- access on component ---

	@Test
	public void testComponentAccessProperty() {
		TableMetadata table = new TableMetadata("PERSON", "Person", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("address", "Address", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("address".equals(field.getName())) {
				var accessAnn = JpaAnnotations.ACCESS.createUsage(ctx);
				accessAnn.value(jakarta.persistence.AccessType.PROPERTY);
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(accessAnn);
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<component name=\"address\""), xml);
		assertTrue(xml.contains("access=\"property\""), xml);
	}

	// --- cascade on any ---

	@Test
	public void testAnyCascade() {
		TableMetadata table = new TableMetadata("PAYMENT", "Payment", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("PAYMENT_TYPE", "paymentType", String.class));
		table.addColumn(new ColumnMetadata("PAYMENT_ID", "paymentId", Long.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		// Remove the basic fields and add an @Any field instead
		ClassDetails objectClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Object.class.getName());
		var fieldType = new org.hibernate.models.internal.ClassTypeDetailsImpl(
				objectClass, org.hibernate.models.spi.TypeDetails.Kind.CLASS);
		var anyField = dc.applyAttribute("payment", fieldType, false, false, ctx);
		var anyAnn = HibernateAnnotations.ANY.createUsage(ctx);
		anyField.addAnnotationUsage(anyAnn);
		var cascadeAnn = HibernateAnnotations.CASCADE.createUsage(ctx);
		cascadeAnn.value(new org.hibernate.annotations.CascadeType[]{
				org.hibernate.annotations.CascadeType.PERSIST,
				org.hibernate.annotations.CascadeType.MERGE});
		anyField.addAnnotationUsage(cascadeAnn);
		var columnAnn = JpaAnnotations.COLUMN.createUsage(ctx);
		columnAnn.name("PAYMENT_TYPE");
		anyField.addAnnotationUsage(columnAnn);
		var jcAnn = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jcAnn.name("PAYMENT_ID");
		anyField.addAnnotationUsage(jcAnn);
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<any name=\"payment\""), xml);
		assertTrue(xml.contains("cascade=\"persist, merge\""), xml);
	}

	// --- access on timestamp ---

	@Test
	public void testTimestampAccessProperty() {
		TableMetadata table = new TableMetadata("ENTITY", "TestEntity", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("LAST_UPDATED", "lastUpdated", java.util.Date.class)
				.version(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("lastUpdated".equals(field.getName())) {
				var accessAnn = JpaAnnotations.ACCESS.createUsage(ctx);
				accessAnn.value(jakarta.persistence.AccessType.PROPERTY);
				((org.hibernate.models.internal.dynamic.DynamicFieldDetails) field)
						.addAnnotationUsage(accessAnn);
			}
		}
		HbmXmlExporter exporter = HbmXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<timestamp"), xml);
		assertTrue(xml.contains("access=\"property\""), xml);
	}
}
