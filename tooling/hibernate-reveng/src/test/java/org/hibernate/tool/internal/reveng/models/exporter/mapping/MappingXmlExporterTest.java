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
package org.hibernate.tool.internal.reveng.models.exporter.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;

import java.util.List;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.EntityListenersJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.jdk.JdkMethodDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.builder.EmbeddableClassBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddableMetadata;
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
 * Tests for {@link MappingXmlExporter}.
 *
 * @author Koen Aers
 */
public class MappingXmlExporterTest {

	private String export(TableMetadata table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		return writer.toString();
	}

	@Test
	public void testXmlHeader() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), xml);
		assertTrue(xml.contains("<entity-mappings"), xml);
		assertTrue(xml.contains("version=\"8.0\""), xml);
		assertTrue(xml.contains("</entity-mappings>"), xml);
	}

	@Test
	public void testEntityAndTable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<entity class=\"com.example.Employee\">"), xml);
		assertTrue(xml.contains("<table name=\"EMPLOYEE\"/>"), xml);
	}

	@Test
	public void testTableWithSchemaAndCatalog() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.setCatalog("MYDB");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("schema=\"HR\""), xml);
		assertTrue(xml.contains("catalog=\"MYDB\""), xml);
	}

	@Test
	public void testIdWithGeneratedValue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		String xml = export(table);
		assertTrue(xml.contains("<id name=\"id\">"), xml);
		assertTrue(xml.contains("<column name=\"ID\""), xml);
		assertTrue(xml.contains("<generated-value strategy=\"IDENTITY\"/>"), xml);
		assertTrue(xml.contains("</id>"), xml);
	}

	@Test
	public void testIdWithoutGeneratedValue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<id name=\"id\">"), xml);
		assertFalse(xml.contains("<generated-value"), xml);
	}

	@Test
	public void testBasicColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String xml = export(table);
		assertTrue(xml.contains("<basic name=\"name\">"), xml);
		assertTrue(xml.contains("<column name=\"NAME\"/>"), xml);
		assertTrue(xml.contains("</basic>"), xml);
	}

	@Test
	public void testColumnAttributes() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		String xml = export(table);
		assertTrue(xml.contains("nullable=\"false\""), xml);
		assertTrue(xml.contains("unique=\"true\""), xml);
		assertTrue(xml.contains("length=\"100\""), xml);
		assertTrue(xml.contains("precision=\"10\""), xml);
		assertTrue(xml.contains("scale=\"2\""), xml);
	}

	@Test
	public void testLobColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("BIO", "bio", String.class).lob(true));
		String xml = export(table);
		assertTrue(xml.contains("<basic name=\"bio\">"), xml);
		assertTrue(xml.contains("<lob/>"), xml);
	}

	@Test
	public void testTemporalColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		String xml = export(table);
		assertTrue(xml.contains("<temporal>TIMESTAMP</temporal>"), xml);
	}

	@Test
	public void testVersionColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		String xml = export(table);
		assertTrue(xml.contains("<version name=\"version\">"), xml);
		assertTrue(xml.contains("<column name=\"VERSION\""), xml);
		assertTrue(xml.contains("</version>"), xml);
		assertFalse(xml.contains("<basic name=\"version\">"), xml);
	}

	@Test
	public void testManyToOne() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<many-to-one name=\"department\" target-entity=\"com.example.Department\""), xml);
		assertTrue(xml.contains("<join-column name=\"DEPT_ID\"/>"), xml);
		assertTrue(xml.contains("</many-to-one>"), xml);
		assertFalse(xml.contains("<basic name=\"deptId\">"), xml);
	}

	@Test
	public void testManyToOneWithFetchAndOptional() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY)
				.optional(false));
		String xml = export(table);
		assertTrue(xml.contains("fetch=\"LAZY\""), xml);
		assertTrue(xml.contains("optional=\"false\""), xml);
	}

	@Test
	public void testManyToOneWithReferencedColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_CODE", "deptCode", String.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE"));
		String xml = export(table);
		assertTrue(xml.contains("referenced-column-name=\"CODE\""), xml);
	}

	@Test
	public void testOneToMany() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<one-to-many name=\"employees\" target-entity=\"com.example.Employee\" mapped-by=\"department\""), xml);
	}

	@Test
	public void testOneToManyWithCascade() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.cascade(CascadeType.ALL));
		String xml = export(table);
		assertTrue(xml.contains("<cascade>"), xml);
		assertTrue(xml.contains("<cascade-all/>"), xml);
		assertTrue(xml.contains("</cascade>"), xml);
		assertTrue(xml.contains("</one-to-many>"), xml);
	}

	@Test
	public void testOneToManyWithFetchAndOrphanRemoval() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER)
				.orphanRemoval(true));
		String xml = export(table);
		assertTrue(xml.contains("fetch=\"EAGER\""), xml);
		assertTrue(xml.contains("orphan-removal=\"true\""), xml);
	}

	@Test
	public void testOneToOneOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		String xml = export(table);
		assertTrue(xml.contains("<one-to-one name=\"address\" target-entity=\"com.example.Address\""), xml);
		assertTrue(xml.contains("<join-column name=\"ADDRESS_ID\"/>"), xml);
		assertTrue(xml.contains("</one-to-one>"), xml);
	}

	@Test
	public void testOneToOneInverse() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address"));
		String xml = export(table);
		assertTrue(xml.contains("mapped-by=\"address\""), xml);
		assertFalse(xml.contains("<join-column"), xml);
	}

	@Test
	public void testManyToManyOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		String xml = export(table);
		assertTrue(xml.contains("<many-to-many name=\"projects\" target-entity=\"com.example.Project\""), xml);
		assertTrue(xml.contains("<join-table name=\"EMPLOYEE_PROJECT\">"), xml);
		assertTrue(xml.contains("<join-column name=\"EMPLOYEE_ID\"/>"), xml);
		assertTrue(xml.contains("<inverse-join-column name=\"PROJECT_ID\"/>"), xml);
		assertTrue(xml.contains("</join-table>"), xml);
		assertTrue(xml.contains("</many-to-many>"), xml);
	}

	@Test
	public void testManyToManyInverse() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects"));
		String xml = export(table);
		assertTrue(xml.contains("mapped-by=\"projects\""), xml);
		assertFalse(xml.contains("<join-table"), xml);
	}

	@Test
	public void testManyToManyWithCascade() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE));
		String xml = export(table);
		assertTrue(xml.contains("<cascade>"), xml);
		assertTrue(xml.contains("<cascade-persist/>"), xml);
		assertTrue(xml.contains("<cascade-merge/>"), xml);
	}

	@Test
	public void testCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		String xml = export(table);
		assertTrue(xml.contains("<embedded-id name=\"id\">"), xml);
		assertTrue(xml.contains("<attribute-override name=\"orderId\">"), xml);
		assertTrue(xml.contains("<column name=\"ORDER_ID\"/>"), xml);
		assertTrue(xml.contains("<attribute-override name=\"lineNumber\">"), xml);
		assertTrue(xml.contains("<column name=\"LINE_NUMBER\"/>"), xml);
		assertTrue(xml.contains("</embedded-id>"), xml);
		assertFalse(xml.contains("<id "), xml);
	}

	@Test
	public void testEmbeddedField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET")
				.addAttributeOverride("city", "HOME_CITY"));
		String xml = export(table);
		assertTrue(xml.contains("<embedded name=\"homeAddress\">"), xml);
		assertTrue(xml.contains("<attribute-override name=\"street\">"), xml);
		assertTrue(xml.contains("<column name=\"HOME_STREET\"/>"), xml);
		assertTrue(xml.contains("<attribute-override name=\"city\">"), xml);
		assertTrue(xml.contains("<column name=\"HOME_CITY\"/>"), xml);
		assertTrue(xml.contains("</embedded>"), xml);
	}

	@Test
	public void testInheritanceRoot() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.STRING)
				.discriminatorColumnLength(31));
		String xml = export(table);
		assertTrue(xml.contains("<inheritance strategy=\"SINGLE_TABLE\"/>"), xml);
		assertTrue(xml.contains("<discriminator-column name=\"DTYPE\""), xml);
		assertTrue(xml.contains("discriminator-type=\"STRING\""), xml);
		assertTrue(xml.contains("length=\"31\""), xml);
	}

	@Test
	public void testInheritanceSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.discriminatorValue("CAR");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		String xml = export(table);
		assertTrue(xml.contains("<discriminator-value>CAR</discriminator-value>"), xml);
		assertTrue(xml.contains("<primary-key-join-column name=\"VEHICLE_ID\"/>"), xml);
	}

	@Test
	public void testSeparateFilesPerEntity() {
		DynamicEntityBuilder builder1 = new DynamicEntityBuilder();
		TableMetadata deptTable = new TableMetadata("DEPARTMENT", "Department", "com.example");
		deptTable.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		deptTable.addColumn(new ColumnMetadata("NAME", "name", String.class));
		ClassDetails dept = builder1.createEntityFromTable(deptTable);

		DynamicEntityBuilder builder2 = new DynamicEntityBuilder();
		TableMetadata empTable = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		empTable.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		ClassDetails emp = builder2.createEntityFromTable(empTable);

		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter deptWriter = new StringWriter();
		exporter.export(deptWriter, dept);
		String deptXml = deptWriter.toString();
		StringWriter empWriter = new StringWriter();
		exporter.export(empWriter, emp);
		String empXml = empWriter.toString();

		assertTrue(deptXml.contains("<entity class=\"com.example.Department\">"), deptXml);
		assertFalse(deptXml.contains("Employee"), deptXml);
		assertTrue(empXml.contains("<entity class=\"com.example.Employee\">"), empXml);
		assertFalse(empXml.contains("Department"), empXml);
	}

	@Test
	public void testCustomTemplatePath(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("main.mapping.ftl"),
				"<!-- Custom mapping for ${helper.getClassName()} -->");
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		ClassDetails entity = builder.createEntityFromTable(table);
		MappingXmlExporter exporter = MappingXmlExporter.create(
				new String[] { tempDir.toString() });
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		assertEquals("<!-- Custom mapping for com.example.Employee -->", writer.toString());
	}

	// --- Embeddable export ---

	private String exportEmbeddable(EmbeddableMetadata metadata) {
		ModelsContext ctx = new BasicModelsContextImpl(
				SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		ClassDetails embeddable = EmbeddableClassBuilder.buildEmbeddableClass(metadata, ctx);
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, embeddable);
		return writer.toString();
	}

	@Test
	public void testEmbeddableExport() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String xml = exportEmbeddable(metadata);
		assertTrue(xml.contains("<embeddable class=\"com.example.OrderLineId\">"), xml);
		assertFalse(xml.contains("<entity "), xml);
	}

	@Test
	public void testEmbeddableExportAttributes() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String xml = exportEmbeddable(metadata);
		assertTrue(xml.contains("<basic name=\"orderId\">"), xml);
		assertTrue(xml.contains("<column name=\"ORDER_ID\""), xml);
		assertTrue(xml.contains("<basic name=\"lineNumber\">"), xml);
		assertTrue(xml.contains("<column name=\"LINE_NUMBER\""), xml);
	}

	@Test
	public void testEmbeddableExportPackage() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class));
		String xml = exportEmbeddable(metadata);
		assertTrue(xml.contains("<package>com.example</package>"), xml);
	}

	@Test
	public void testEmbeddableExportNoEntityElements() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class));
		String xml = exportEmbeddable(metadata);
		assertFalse(xml.contains("<filter-def"), xml);
		assertFalse(xml.contains("<named-query"), xml);
		assertFalse(xml.contains("<named-native-query"), xml);
	}

	// --- @EntityListeners ---

	@Test
	public void testEntityListenersElement() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityListenersJpaAnnotation el = JpaAnnotations.ENTITY_LISTENERS.createUsage(builder.getModelsContext());
		el.value(new Class<?>[] { java.io.Serializable.class, java.lang.Comparable.class });
		((MutableAnnotationTarget) entity).addAnnotationUsage(el);
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<entity-listeners>"), xml);
		assertTrue(xml.contains("<entity-listener class=\"java.io.Serializable\""), xml);
		assertTrue(xml.contains("<entity-listener class=\"java.lang.Comparable\""), xml);
		assertTrue(xml.contains("</entity-listeners>"), xml);
	}

	// --- Lifecycle callbacks ---

	static class WithCallbacks {
		@jakarta.persistence.PrePersist
		void onPrePersist() {}
		@jakarta.persistence.PostLoad
		void onPostLoad() {}
	}

	@Test
	public void testLifecycleCallbackElements() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		for (java.lang.reflect.Method method : WithCallbacks.class.getDeclaredMethods()) {
			dc.addMethod(new JdkMethodDetails(
					method, MethodDetails.MethodKind.OTHER, null, dc, builder.getModelsContext()));
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<pre-persist method-name=\"onPrePersist\"/>"), xml);
		assertTrue(xml.contains("<post-load method-name=\"onPostLoad\"/>"), xml);
	}

	// --- Access type ---

	@Test
	public void testEntityAccessTypeProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation access =
				JpaAnnotations.ACCESS.createUsage(builder.getModelsContext());
		access.value(jakarta.persistence.AccessType.PROPERTY);
		((DynamicClassDetails) entity).addAnnotationUsage(access);
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("access=\"PROPERTY\""), xml);
	}

	@Test
	public void testEntityAccessTypeDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertFalse(xml.contains("access="), xml);
	}

	@Test
	public void testFieldAccessTypeProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		// Find the "name" field and add @Access(PROPERTY)
		for (var field : entity.getFields()) {
			if ("name".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation access =
						JpaAnnotations.ACCESS.createUsage(builder.getModelsContext());
				access.value(jakarta.persistence.AccessType.PROPERTY);
				((MutableAnnotationTarget) field).addAnnotationUsage(access);
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<basic name=\"name\" access=\"PROPERTY\""), xml);
	}

	// --- @SQLDeleteAll ---

	@Test
	public void testSQLDeleteAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation sda =
				HibernateAnnotations.SQL_DELETE_ALL.createUsage(builder.getModelsContext());
		sda.sql("DELETE FROM EMPLOYEE WHERE dept_id = ?");
		((DynamicClassDetails) entity).addAnnotationUsage(sda);
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<sql-delete-all>DELETE FROM EMPLOYEE WHERE dept_id = ?</sql-delete-all>"), xml);
	}
}
