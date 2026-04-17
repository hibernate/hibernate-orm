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
package org.hibernate.tool.internal.exporter.mapping;

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
import org.hibernate.tool.internal.reveng.models.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.builder.db.EmbeddableClassBuilder;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.EmbeddableDescriptor;
import org.hibernate.tool.internal.descriptor.CompositeIdDescriptor;
import org.hibernate.tool.internal.descriptor.EmbeddedFieldDescriptor;
import org.hibernate.tool.internal.descriptor.ForeignKeyDescriptor;
import org.hibernate.tool.internal.descriptor.InheritanceDescriptor;
import org.hibernate.tool.internal.descriptor.ManyToManyDescriptor;
import org.hibernate.tool.internal.descriptor.OneToManyDescriptor;
import org.hibernate.tool.internal.descriptor.OneToOneDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MappingXmlExporter}.
 *
 * @author Koen Aers
 */
public class MappingXmlExporterTest {

	private String export(TableDescriptor table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		return writer.toString();
	}

	@Test
	public void testXmlHeader() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), xml);
		assertTrue(xml.contains("<entity-mappings"), xml);
		assertTrue(xml.contains("version=\"8.0\""), xml);
		assertTrue(xml.contains("</entity-mappings>"), xml);
	}

	@Test
	public void testGeneratedHeader() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.matches("(?s).*<!-- Generated .+ by Hibernate Tools .+ -->.*"), xml);
	}

	@Test
	public void testEntityAndTable() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<entity class=\"com.example.Employee\">"), xml);
		assertTrue(xml.contains("<table name=\"EMPLOYEE\"/>"), xml);
	}

	@Test
	public void testTableWithSchemaAndCatalog() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.setCatalog("MYDB");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("schema=\"HR\""), xml);
		assertTrue(xml.contains("catalog=\"MYDB\""), xml);
	}

	@Test
	public void testIdWithGeneratedValue() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
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
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertTrue(xml.contains("<id name=\"id\">"), xml);
		assertFalse(xml.contains("<generated-value"), xml);
	}

	@Test
	public void testBasicColumn() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		String xml = export(table);
		assertTrue(xml.contains("<basic name=\"name\">"), xml);
		assertTrue(xml.contains("<column name=\"NAME\"/>"), xml);
		assertTrue(xml.contains("</basic>"), xml);
	}

	@Test
	public void testColumnAttributes() {
		TableDescriptor table = new TableDescriptor("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		table.addColumn(new ColumnDescriptor("PRICE", "price", BigDecimal.class)
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
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("BIO", "bio", String.class).lob(true));
		String xml = export(table);
		assertTrue(xml.contains("<basic name=\"bio\">"), xml);
		assertTrue(xml.contains("<lob/>"), xml);
	}

	@Test
	public void testTemporalColumn() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		String xml = export(table);
		assertTrue(xml.contains("<temporal>TIMESTAMP</temporal>"), xml);
	}

	@Test
	public void testVersionColumn() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("VERSION", "version", Integer.class).version(true));
		String xml = export(table);
		assertTrue(xml.contains("<version name=\"version\">"), xml);
		assertTrue(xml.contains("<column name=\"VERSION\""), xml);
		assertTrue(xml.contains("</version>"), xml);
		assertFalse(xml.contains("<basic name=\"version\">"), xml);
	}

	@Test
	public void testManyToOne() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyDescriptor(
				"department", "DEPT_ID", "Department", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<many-to-one name=\"department\" target-entity=\"com.example.Department\""), xml);
		assertTrue(xml.contains("<join-column name=\"DEPT_ID\"/>"), xml);
		assertTrue(xml.contains("</many-to-one>"), xml);
		assertFalse(xml.contains("<basic name=\"deptId\">"), xml);
	}

	@Test
	public void testManyToOneWithFetchAndOptional() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyDescriptor(
				"department", "DEPT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY)
				.optional(false));
		String xml = export(table);
		assertTrue(xml.contains("fetch=\"LAZY\""), xml);
		assertTrue(xml.contains("optional=\"false\""), xml);
	}

	@Test
	public void testManyToOneWithReferencedColumn() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DEPT_CODE", "deptCode", String.class));
		table.addForeignKey(new ForeignKeyDescriptor(
				"department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE"));
		String xml = export(table);
		assertTrue(xml.contains("referenced-column-name=\"CODE\""), xml);
	}

	@Test
	public void testOneToMany() {
		TableDescriptor table = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyDescriptor(
				"employees", "department", "Employee", "com.example"));
		String xml = export(table);
		assertTrue(xml.contains("<one-to-many name=\"employees\" target-entity=\"com.example.Employee\" mapped-by=\"department\""), xml);
	}

	@Test
	public void testOneToManyWithCascade() {
		TableDescriptor table = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyDescriptor(
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
		TableDescriptor table = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyDescriptor(
				"employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER)
				.orphanRemoval(true));
		String xml = export(table);
		assertTrue(xml.contains("fetch=\"EAGER\""), xml);
		assertTrue(xml.contains("orphan-removal=\"true\""), xml);
	}

	@Test
	public void testOneToOneOwning() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneDescriptor("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		String xml = export(table);
		assertTrue(xml.contains("<one-to-one name=\"address\" target-entity=\"com.example.Address\""), xml);
		assertTrue(xml.contains("<join-column name=\"ADDRESS_ID\"/>"), xml);
		assertTrue(xml.contains("</one-to-one>"), xml);
	}

	@Test
	public void testOneToOneInverse() {
		TableDescriptor table = new TableDescriptor("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneDescriptor("employee", "Employee", "com.example")
				.mappedBy("address"));
		String xml = export(table);
		assertTrue(xml.contains("mapped-by=\"address\""), xml);
		assertFalse(xml.contains("<join-column"), xml);
	}

	@Test
	public void testManyToManyOwning() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyDescriptor("projects", "Project", "com.example")
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
		TableDescriptor table = new TableDescriptor("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyDescriptor("employees", "Employee", "com.example")
				.mappedBy("projects"));
		String xml = export(table);
		assertTrue(xml.contains("mapped-by=\"projects\""), xml);
		assertFalse(xml.contains("<join-table"), xml);
	}

	@Test
	public void testManyToManyWithCascade() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyDescriptor("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE));
		String xml = export(table);
		assertTrue(xml.contains("<cascade>"), xml);
		assertTrue(xml.contains("<cascade-persist/>"), xml);
		assertTrue(xml.contains("<cascade-merge/>"), xml);
	}

	@Test
	public void testCompositeId() {
		TableDescriptor table = new TableDescriptor("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdDescriptor("id", "OrderLineId", "com.example")
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
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldDescriptor("homeAddress", "Address", "com.example")
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
		TableDescriptor table = new TableDescriptor("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceDescriptor(InheritanceType.SINGLE_TABLE)
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
		TableDescriptor table = new TableDescriptor("CAR", "Car", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
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
		TableDescriptor deptTable = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		deptTable.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		deptTable.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		ClassDetails dept = builder1.createEntityFromTable(deptTable);

		DynamicEntityBuilder builder2 = new DynamicEntityBuilder();
		TableDescriptor empTable = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		empTable.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
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
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		ClassDetails entity = builder.createEntityFromTable(table);
		MappingXmlExporter exporter = MappingXmlExporter.create(
				new String[] { tempDir.toString() });
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		assertEquals("<!-- Custom mapping for com.example.Employee -->", writer.toString());
	}

	// --- Embeddable export ---

	private String exportEmbeddable(EmbeddableDescriptor metadata) {
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
		EmbeddableDescriptor metadata = new EmbeddableDescriptor("OrderLineId", "com.example")
				.addColumn(new ColumnDescriptor("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnDescriptor("LINE_NUMBER", "lineNumber", Integer.class));
		String xml = exportEmbeddable(metadata);
		assertTrue(xml.contains("<embeddable class=\"com.example.OrderLineId\">"), xml);
		assertFalse(xml.contains("<entity "), xml);
	}

	@Test
	public void testEmbeddableExportAttributes() {
		EmbeddableDescriptor metadata = new EmbeddableDescriptor("OrderLineId", "com.example")
				.addColumn(new ColumnDescriptor("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnDescriptor("LINE_NUMBER", "lineNumber", Integer.class));
		String xml = exportEmbeddable(metadata);
		assertTrue(xml.contains("<basic name=\"orderId\">"), xml);
		assertTrue(xml.contains("<column name=\"ORDER_ID\""), xml);
		assertTrue(xml.contains("<basic name=\"lineNumber\">"), xml);
		assertTrue(xml.contains("<column name=\"LINE_NUMBER\""), xml);
	}

	@Test
	public void testEmbeddableExportPackage() {
		EmbeddableDescriptor metadata = new EmbeddableDescriptor("OrderLineId", "com.example")
				.addColumn(new ColumnDescriptor("ORDER_ID", "orderId", Long.class));
		String xml = exportEmbeddable(metadata);
		assertTrue(xml.contains("<package>com.example</package>"), xml);
	}

	@Test
	public void testEmbeddableExportNoEntityElements() {
		EmbeddableDescriptor metadata = new EmbeddableDescriptor("OrderLineId", "com.example")
				.addColumn(new ColumnDescriptor("ORDER_ID", "orderId", Long.class));
		String xml = exportEmbeddable(metadata);
		assertFalse(xml.contains("<filter-def"), xml);
		assertFalse(xml.contains("<named-query"), xml);
		assertFalse(xml.contains("<named-native-query"), xml);
	}

	// --- @EntityListeners ---

	@Test
	public void testEntityListenersElement() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
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
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
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
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
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
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		String xml = export(table);
		assertFalse(xml.contains("access="), xml);
	}

	@Test
	public void testFieldAccessTypeProperty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
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

	// --- Column insertable/updatable ---

	@Test
	public void testColumnInsertableFalse() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class).insertable(false));
		String xml = export(table);
		assertTrue(xml.contains("insertable=\"false\""), xml);
	}

	@Test
	public void testColumnUpdatableFalse() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class).updatable(false));
		String xml = export(table);
		assertTrue(xml.contains("updatable=\"false\""), xml);
	}

	@Test
	public void testColumnInsertableUpdatableDefault() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		String xml = export(table);
		assertFalse(xml.contains("insertable="), xml);
		assertFalse(xml.contains("updatable="), xml);
	}

	// --- Generator ---

	@Test
	public void testGeneratorWithSequenceGenerator() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		for (var field : entity.getFields()) {
			if ("id".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation gv =
						JpaAnnotations.GENERATED_VALUE.createUsage(builder.getModelsContext());
				gv.strategy(GenerationType.SEQUENCE);
				gv.generator("emp_seq");
				((MutableAnnotationTarget) field).addAnnotationUsage(gv);
				org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation sg =
						JpaAnnotations.SEQUENCE_GENERATOR.createUsage(builder.getModelsContext());
				sg.name("emp_seq");
				sg.sequenceName("EMPLOYEE_SEQ");
				sg.allocationSize(20);
				((MutableAnnotationTarget) field).addAnnotationUsage(sg);
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("generator=\"emp_seq\""), xml);
		assertTrue(xml.contains("<sequence-generator name=\"emp_seq\" sequence-name=\"EMPLOYEE_SEQ\" allocation-size=\"20\""), xml);
	}

	@Test
	public void testGeneratorWithTableGenerator() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		for (var field : entity.getFields()) {
			if ("id".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation gv =
						JpaAnnotations.GENERATED_VALUE.createUsage(builder.getModelsContext());
				gv.strategy(GenerationType.TABLE);
				gv.generator("emp_gen");
				((MutableAnnotationTarget) field).addAnnotationUsage(gv);
				org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation tg =
						JpaAnnotations.TABLE_GENERATOR.createUsage(builder.getModelsContext());
				tg.name("emp_gen");
				tg.table("ID_GEN");
				tg.pkColumnName("GEN_NAME");
				((MutableAnnotationTarget) field).addAnnotationUsage(tg);
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("generator=\"emp_gen\""), xml);
		assertTrue(xml.contains("<table-generator name=\"emp_gen\" table=\"ID_GEN\" pk-column-name=\"GEN_NAME\""), xml);
	}

	// --- Column definition ---

	@Test
	public void testColumnDefinition() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("BIO", "bio", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		for (var field : entity.getFields()) {
			if ("bio".equals(field.getName())) {
				org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation col =
						JpaAnnotations.COLUMN.createUsage(builder.getModelsContext());
				col.name("BIO");
				col.columnDefinition("TEXT NOT NULL");
				((MutableAnnotationTarget) field).addAnnotationUsage(col);
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("column-definition=\"TEXT NOT NULL\""), xml);
	}

	@Test
	public void testColumnDefinitionDefault() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		String xml = export(table);
		assertFalse(xml.contains("column-definition="), xml);
	}

	// --- @SQLDeleteAll ---

	@Test
	public void testSQLDeleteAll() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
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

	// --- Multiple @JoinColumns ---

	@Test
	public void testManyToOneMultipleJoinColumns() {
		TableDescriptor table = new TableDescriptor("ORDER_ITEM", "OrderItem", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addForeignKey(new ForeignKeyDescriptor(
				"order", "ORDER_ID", "Order", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
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
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<join-column name=\"ORDER_ID\"/>"), xml);
		assertTrue(xml.contains("<join-column name=\"ORDER_SEQ\"/>"), xml);
	}

	@Test
	public void testManyToOneMultipleJoinColumnsWithReferencedColumn() {
		TableDescriptor table = new TableDescriptor("ORDER_ITEM", "OrderItem", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addForeignKey(new ForeignKeyDescriptor(
				"order", "ORDER_ID", "Order", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("order".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				var jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc1.name("ORDER_ID");
				jc1.referencedColumnName("ID");
				var jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc2.name("ORDER_SEQ");
				jc2.referencedColumnName("SEQ_NUM");
				var jcs = JpaAnnotations.JOIN_COLUMNS.createUsage(ctx);
				jcs.value(new jakarta.persistence.JoinColumn[]{jc1, jc2});
				df.addAnnotationUsage(jcs);
				break;
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<join-column name=\"ORDER_ID\" referenced-column-name=\"ID\"/>"), xml);
		assertTrue(xml.contains("<join-column name=\"ORDER_SEQ\" referenced-column-name=\"SEQ_NUM\"/>"), xml);
	}

	@Test
	public void testOneToOneMultipleJoinColumns() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneDescriptor("address", "Address", "com.example")
				.foreignKeyColumnName("ADDR_ID"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("address".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				var jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc1.name("ADDR_ID");
				var jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc2.name("ADDR_VERSION");
				var jcs = JpaAnnotations.JOIN_COLUMNS.createUsage(ctx);
				jcs.value(new jakarta.persistence.JoinColumn[]{jc1, jc2});
				df.addAnnotationUsage(jcs);
				break;
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<join-column name=\"ADDR_ID\"/>"), xml);
		assertTrue(xml.contains("<join-column name=\"ADDR_VERSION\"/>"), xml);
		assertTrue(xml.contains("</one-to-one>"), xml);
	}

	// --- Multiple @PrimaryKeyJoinColumns ---

	@Test
	public void testMultiplePrimaryKeyJoinColumns() {
		TableDescriptor table = new TableDescriptor("CAR", "Car", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Replace single @PrimaryKeyJoinColumn with @PrimaryKeyJoinColumns
		var pkjc1 = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc1.name("VEHICLE_ID");
		var pkjc2 = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc2.name("VEHICLE_SITE");
		var pkjcs = new org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnsJpaAnnotation(
			java.util.Map.of("value", new jakarta.persistence.PrimaryKeyJoinColumn[]{pkjc1, pkjc2}), ctx);
		((DynamicClassDetails) entity).addAnnotationUsage(pkjcs);
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<primary-key-join-column name=\"VEHICLE_ID\"/>"), xml);
		assertTrue(xml.contains("<primary-key-join-column name=\"VEHICLE_SITE\"/>"), xml);
	}

	// --- Multiple join columns in join table ---

	@Test
	public void testManyToManyMultipleJoinTableColumns() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyDescriptor("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("projects".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				// Replace join table with multi-column version
				var jt = JpaAnnotations.JOIN_TABLE.createUsage(ctx);
				jt.name("EMPLOYEE_PROJECT");
				var jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc1.name("EMP_ID");
				var jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc2.name("EMP_DEPT");
				jt.joinColumns(new jakarta.persistence.JoinColumn[]{jc1, jc2});
				var ijc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				ijc1.name("PROJ_ID");
				var ijc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				ijc2.name("PROJ_VERSION");
				jt.inverseJoinColumns(new jakarta.persistence.JoinColumn[]{ijc1, ijc2});
				df.addAnnotationUsage(jt);
				break;
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<join-column name=\"EMP_ID\"/>"), xml);
		assertTrue(xml.contains("<join-column name=\"EMP_DEPT\"/>"), xml);
		assertTrue(xml.contains("<inverse-join-column name=\"PROJ_ID\"/>"), xml);
		assertTrue(xml.contains("<inverse-join-column name=\"PROJ_VERSION\"/>"), xml);
	}

	@Test
	public void testJoinTableSchemaCatalog() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyDescriptor("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("projects".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				// Replace the JoinTable annotation with one that has schema/catalog
				var jt = JpaAnnotations.JOIN_TABLE.createUsage(ctx);
				jt.name("EMPLOYEE_PROJECT");
				jt.schema("HR");
				jt.catalog("CORP");
				var jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				jc.name("EMPLOYEE_ID");
				jt.joinColumns(new jakarta.persistence.JoinColumn[]{jc});
				var ijc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
				ijc.name("PROJECT_ID");
				jt.inverseJoinColumns(new jakarta.persistence.JoinColumn[]{ijc});
				df.addAnnotationUsage(jt);
				break;
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("schema=\"HR\""), xml);
		assertTrue(xml.contains("catalog=\"CORP\""), xml);
		assertTrue(xml.contains("<join-table name=\"EMPLOYEE_PROJECT\""), xml);
	}

	@Test
	public void testMapKeyJoinColumn() {
		TableDescriptor table = new TableDescriptor("ORDERS", "Order", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyDescriptor("items", "OrderItem", "com.example", "order"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		// Replace the field type with Map<Product, OrderItem> and add @MapKeyJoinColumn
		for (var field : entity.getFields()) {
			if ("items".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				var mkjc = JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage(ctx);
				mkjc.name("PRODUCT_ID");
				df.addAnnotationUsage(mkjc);
				break;
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<map-key-join-column name=\"PRODUCT_ID\"/>"), xml);
	}

	@Test
	public void testFormulaOnManyToOne() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyDescriptor(
				"department", "DEPT_ID", "Department", "com.example"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("department".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				var formula = HibernateAnnotations.FORMULA.createUsage(ctx);
				formula.value("(SELECT d.ID FROM DEPARTMENT d WHERE d.CODE = DEPT_CODE)");
				df.addAnnotationUsage(formula);
				break;
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<formula>(SELECT d.ID FROM DEPARTMENT d WHERE d.CODE = DEPT_CODE)</formula>"), xml);
		assertFalse(xml.contains("<join-column name=\"DEPT_ID\""), xml);
	}

	@Test
	public void testFormulaOnOneToOne() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneDescriptor("badge", "Badge", "com.example")
				.foreignKeyColumnName("BADGE_ID"));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		ModelsContext ctx = builder.getModelsContext();
		for (var field : entity.getFields()) {
			if ("badge".equals(field.getName())) {
				var df = (org.hibernate.models.internal.dynamic.DynamicFieldDetails) field;
				var formula = HibernateAnnotations.FORMULA.createUsage(ctx);
				formula.value("(SELECT b.ID FROM BADGE b WHERE b.EMP_ID = ID)");
				df.addAnnotationUsage(formula);
				break;
			}
		}
		MappingXmlExporter exporter = MappingXmlExporter.create();
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String xml = writer.toString();
		assertTrue(xml.contains("<formula>(SELECT b.ID FROM BADGE b WHERE b.EMP_ID = ID)</formula>"), xml);
		assertFalse(xml.contains("<join-column name=\"BADGE_ID\""), xml);
	}
}
