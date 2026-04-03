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
package org.hibernate.tool.internal.reveng.models.exporter.ddl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DdlExporter}.
 *
 * @author Koen Aers
 */
public class DdlExporterTest {

	private Properties defaultProperties() {
		Properties props = new Properties();
		props.put(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
		return props;
	}

	private ClassDetails buildEntity(DynamicEntityBuilder builder,
									  String tableName, String className, String pkg) {
		TableMetadata table = new TableMetadata(tableName, className, pkg);
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		return builder.createEntityFromTable(table);
	}

	@Test
	public void testExportCreateDdl() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties());
		StringWriter writer = new StringWriter();
		exporter.exportCreateDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.toLowerCase().contains("create table"), ddl);
		assertTrue(ddl.toLowerCase().contains("employee"), ddl);
		assertTrue(ddl.toLowerCase().contains("id"), ddl);
		assertTrue(ddl.toLowerCase().contains("name"), ddl);
	}

	@Test
	public void testExportDropDdl() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties());
		StringWriter writer = new StringWriter();
		exporter.exportDropDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.toLowerCase().contains("drop"), ddl);
		assertTrue(ddl.toLowerCase().contains("employee"), ddl);
	}

	@Test
	public void testExportBothDdl() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties());
		StringWriter writer = new StringWriter();
		exporter.exportBothDdl(writer);
		String ddl = writer.toString();
		int dropPos = ddl.toLowerCase().indexOf("drop");
		int createPos = ddl.toLowerCase().indexOf("create table");
		assertTrue(dropPos >= 0, "Should contain DROP: " + ddl);
		assertTrue(createPos >= 0, "Should contain CREATE TABLE: " + ddl);
		assertTrue(dropPos < createPos,
				"DROP should appear before CREATE: " + ddl);
	}

	@Test
	public void testCustomDelimiter() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties())
				.delimiter("$$");
		StringWriter writer = new StringWriter();
		exporter.exportCreateDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.contains("$$"), ddl);
	}

	@Test
	public void testDefaultDelimiter() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties());
		StringWriter writer = new StringWriter();
		exporter.exportCreateDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.contains(";"), ddl);
	}

	@Test
	public void testMultipleEntities() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		ClassDetails department = buildEntity(builder, "DEPARTMENT", "Department", "com.example");
		DdlExporter exporter = DdlExporter.create(
				List.of(employee, department), defaultProperties());
		StringWriter writer = new StringWriter();
		exporter.exportCreateDdl(writer);
		String ddl = writer.toString().toLowerCase();
		assertTrue(ddl.contains("employee"), ddl);
		assertTrue(ddl.contains("department"), ddl);
	}

	@Test
	public void testColumnTypes() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("PRICE", "price", java.math.BigDecimal.class));
		table.addColumn(new ColumnMetadata("ACTIVE", "active", Boolean.class));
		ClassDetails entity = builder.createEntityFromTable(table);
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties());
		StringWriter writer = new StringWriter();
		exporter.exportCreateDdl(writer);
		String ddl = writer.toString().toLowerCase();
		assertTrue(ddl.contains("product"), ddl);
		assertTrue(ddl.contains("price"), ddl);
		assertTrue(ddl.contains("active"), ddl);
	}
}
