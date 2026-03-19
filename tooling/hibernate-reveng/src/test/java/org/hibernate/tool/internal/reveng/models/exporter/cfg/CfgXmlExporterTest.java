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
package org.hibernate.tool.internal.reveng.models.exporter.cfg;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CfgXmlExporter}.
 *
 * @author Koen Aers
 */
public class CfgXmlExporterTest {

	private ClassDetails buildEntity(DynamicEntityBuilder builder, String tableName,
									 String className, String pkg) {
		TableMetadata table = new TableMetadata(tableName, className, pkg);
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		return builder.createEntityFromTable(table);
	}

	@Test
	public void testEmptySchema() {
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of());
		StringWriter writer = new StringWriter();
		exporter.export(writer, new Properties());
		String xml = writer.toString();
		assertTrue(xml.contains("<hibernate-configuration>"));
		assertTrue(xml.contains("<session-factory>"));
		assertTrue(xml.contains("</session-factory>"));
		assertTrue(xml.contains("</hibernate-configuration>"));
		assertFalse(xml.contains("<mapping"));
	}

	@Test
	public void testSingleEntity() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of(entity));
		StringWriter writer = new StringWriter();
		Properties props = new Properties();
		props.put("ejb3", "true");
		exporter.export(writer, props);
		String xml = writer.toString();
		assertTrue(xml.contains("<mapping class=\"com.example.Employee\"/>"));
	}

	@Test
	public void testSingleEntityResourceMapping() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of(entity));
		StringWriter writer = new StringWriter();
		exporter.export(writer, new Properties());
		String xml = writer.toString();
		assertTrue(xml.contains("<mapping resource=\"com/example/Employee.hbm.xml\"/>"));
	}

	@Test
	public void testMultipleEntities() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		ClassDetails department = buildEntity(builder, "DEPARTMENT", "Department", "com.example");
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of(employee, department));
		StringWriter writer = new StringWriter();
		Properties props = new Properties();
		props.put("ejb3", "true");
		exporter.export(writer, props);
		String xml = writer.toString();
		assertTrue(xml.contains("<mapping class=\"com.example.Employee\"/>"));
		assertTrue(xml.contains("<mapping class=\"com.example.Department\"/>"));
	}

	@Test
	public void testHibernateProperties() {
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of());
		StringWriter writer = new StringWriter();
		Properties props = new Properties();
		props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		props.put("hibernate.show_sql", "true");
		exporter.export(writer, props);
		String xml = writer.toString();
		assertTrue(xml.contains("<property name=\"hibernate.dialect\">org.hibernate.dialect.H2Dialect</property>"));
		assertTrue(xml.contains("<property name=\"hibernate.show_sql\">true</property>"));
	}

	@Test
	public void testIgnoredProperties() {
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of());
		StringWriter writer = new StringWriter();
		Properties props = new Properties();
		props.put(Environment.SESSION_FACTORY_NAME, "myFactory");
		props.put(Environment.HBM2DDL_AUTO, "false");
		props.put("hibernate.temp.use_jdbc_metadata_defaults", "true");
		props.put(Environment.TRANSACTION_COORDINATOR_STRATEGY,
				"org.hibernate.console.FakeTransactionManagerLookup");
		exporter.export(writer, props);
		String xml = writer.toString();
		assertFalse(xml.contains("<property name=\"" + Environment.SESSION_FACTORY_NAME + "\">"));
		assertFalse(xml.contains("<property name=\"" + Environment.HBM2DDL_AUTO + "\">"));
		assertFalse(xml.contains("<property name=\"hibernate.temp.use_jdbc_metadata_defaults\">"));
		assertFalse(xml.contains("<property name=\"" + Environment.TRANSACTION_COORDINATOR_STRATEGY + "\">"));
	}

	@Test
	public void testSessionFactoryName() {
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of());
		StringWriter writer = new StringWriter();
		Properties props = new Properties();
		props.put(Environment.SESSION_FACTORY_NAME, "mySessionFactory");
		exporter.export(writer, props);
		String xml = writer.toString();
		assertTrue(xml.contains("<session-factory name=\"mySessionFactory\">"));
	}

	@Test
	public void testInheritanceHierarchy() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata rootTable = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		rootTable.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		ClassDetails root = builder.createEntityFromTable(rootTable);
		TableMetadata childTable = new TableMetadata("CAR", "Car", "com.example");
		childTable.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		childTable.parent("Vehicle", "com.example");
		ClassDetails child = builder.createEntityFromTable(childTable);
		List<ClassDetails> entities = new ArrayList<>();
		entities.add(child);
		entities.add(root);
		CfgXmlExporter exporter = CfgXmlExporter.create(entities);
		StringWriter writer = new StringWriter();
		Properties props = new Properties();
		props.put("ejb3", "true");
		exporter.export(writer, props);
		String xml = writer.toString();
		int rootIdx = xml.indexOf("<mapping class=\"com.example.Vehicle\"/>");
		int childIdx = xml.indexOf("<mapping class=\"com.example.Car\"/>");
		assertTrue(rootIdx >= 0, "Root entity should be present");
		assertTrue(childIdx >= 0, "Child entity should be present");
		assertTrue(rootIdx < childIdx, "Root entity should appear before child entity");
	}

	@Test
	public void testXmlEscaping() {
		CfgXmlExporter exporter = CfgXmlExporter.create(List.of());
		StringWriter writer = new StringWriter();
		Properties props = new Properties();
		props.put("hibernate.custom", "value <with> special chars");
		exporter.export(writer, props);
		String xml = writer.toString();
		assertTrue(xml.contains("value &lt;with&gt; special chars"));
		assertFalse(xml.contains("value <with> special chars"));
	}

}
