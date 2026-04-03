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

import java.io.File;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DdlExporter}.
 *
 * @author Koen Aers
 */
public class DdlExporterTest {

	private static final String DB_PATH = "./target/test-ddl-exporter";
	private static final String H2_FILE_URL = "jdbc:h2:" + DB_PATH;

	private Properties defaultProperties() {
		Properties props = new Properties();
		props.put(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
		return props;
	}

	private Properties h2FileProperties() {
		Properties props = new Properties();
		props.put(AvailableSettings.URL, H2_FILE_URL);
		props.put(AvailableSettings.DRIVER, "org.h2.Driver");
		props.put(AvailableSettings.USER, "sa");
		props.put(AvailableSettings.PASS, "");
		return props;
	}

	private ClassDetails buildEntity(DynamicEntityBuilder builder,
									  String tableName, String className, String pkg) {
		TableMetadata table = new TableMetadata(tableName, className, pkg);
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		return builder.createEntityFromTable(table);
	}

	@BeforeEach
	public void setUp() {
		deleteDbFiles();
	}

	@AfterEach
	public void tearDown() {
		deleteDbFiles();
	}

	private void deleteDbFiles() {
		new File(DB_PATH + ".mv.db").delete();
		new File(DB_PATH + ".trace.db").delete();
	}

	private Connection openFileConnection() throws SQLException {
		return DriverManager.getConnection(H2_FILE_URL, "sa", "");
	}

	// ---- Script export tests ----

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

	// ---- File export tests ----

	@Test
	public void testExportCreateDdlToFile() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties());
		File outputFile = new File("./target/test-create.ddl");
		try {
			exporter.exportCreateDdl(outputFile);
			assertTrue(outputFile.exists(), "DDL file should exist");
			String ddl = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()))
					.toLowerCase();
			assertTrue(ddl.contains("create table"), ddl);
			assertTrue(ddl.contains("employee"), ddl);
		}
		catch (java.io.IOException e) {
			fail("Unexpected IOException: " + e.getMessage());
		}
		finally {
			outputFile.delete();
		}
	}

	@Test
	public void testExportDropDdlToFile() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMPLOYEE", "Employee", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), defaultProperties());
		File outputFile = new File("./target/test-drop.ddl");
		try {
			exporter.exportDropDdl(outputFile);
			assertTrue(outputFile.exists(), "DDL file should exist");
			String ddl = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()))
					.toLowerCase();
			assertTrue(ddl.contains("drop"), ddl);
			assertTrue(ddl.contains("employee"), ddl);
		}
		catch (java.io.IOException e) {
			fail("Unexpected IOException: " + e.getMessage());
		}
		finally {
			outputFile.delete();
		}
	}

	// ---- Database execution tests ----

	@Test
	public void testExecuteCreateDdl() throws SQLException {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMP_CREATE", "EmpCreate", "com.example");
		Properties props = h2FileProperties();
		DdlExporter exporter = DdlExporter.create(List.of(entity), props);
		exporter.executeCreateDdl();
		try (Connection conn = openFileConnection();
			 Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
					+ "WHERE TABLE_NAME = 'EMP_CREATE'");
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1), "Table EMP_CREATE should exist");
		}
	}

	@Test
	public void testExecuteDropDdl() throws SQLException {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMP_DROP", "EmpDrop", "com.example");
		Properties props = h2FileProperties();
		DdlExporter exporter = DdlExporter.create(List.of(entity), props);
		exporter.executeCreateDdl();
		exporter.executeDropDdl();
		try (Connection conn = openFileConnection();
			 Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
					+ "WHERE TABLE_NAME = 'EMP_DROP'");
			assertTrue(rs.next());
			assertEquals(0, rs.getInt(1), "Table EMP_DROP should not exist");
		}
	}

	@Test
	public void testExecuteBothDdl() throws SQLException {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(builder, "EMP_BOTH", "EmpBoth", "com.example");
		Properties props = h2FileProperties();
		DdlExporter exporter = DdlExporter.create(List.of(entity), props);
		exporter.executeCreateDdl();
		exporter.executeBothDdl();
		try (Connection conn = openFileConnection();
			 Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
					+ "WHERE TABLE_NAME = 'EMP_BOTH'");
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1),
					"Table EMP_BOTH should exist after executeBothDdl");
		}
	}

	// ---- Schema update/migration tests ----

	@Test
	public void testExportUpdateDdl() throws SQLException {
		Properties props = h2FileProperties();
		// Create a table with only ID column
		try (Connection conn = openFileConnection();
			 Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE EMP_UPDATE (ID BIGINT PRIMARY KEY)");
		}
		// Build entity with ID + NAME — migration should add NAME
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMP_UPDATE", "EmpUpdate", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), props);
		StringWriter writer = new StringWriter();
		exporter.exportUpdateDdl(writer);
		String ddl = writer.toString().toLowerCase();
		assertTrue(ddl.contains("alter"), "Should contain ALTER: " + ddl);
		assertTrue(ddl.contains("name"), "Should contain NAME column: " + ddl);
	}

	@Test
	public void testExecuteUpdateDdl() throws SQLException {
		Properties props = h2FileProperties();
		// Create a table with only ID column
		try (Connection conn = openFileConnection();
			 Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE EMP_MIGRATE (ID BIGINT PRIMARY KEY)");
		}
		// Build entity with ID + NAME
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMP_MIGRATE", "EmpMigrate", "com.example");
		DdlExporter exporter = DdlExporter.create(List.of(entity), props);
		exporter.executeUpdateDdl();
		// Verify the NAME column was added
		try (Connection conn = openFileConnection();
			 Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
					+ "WHERE TABLE_NAME = 'EMP_MIGRATE' AND COLUMN_NAME = 'NAME'");
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1),
					"NAME column should exist after migration");
		}
	}

	// ---- Halt on error tests ----

	@Test
	public void testHaltOnErrorThrowsOnDuplicateCreate() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMP_HALT", "EmpHalt", "com.example");
		Properties props = h2FileProperties();
		DdlExporter exporter = DdlExporter.create(List.of(entity), props)
				.haltOnError(true);
		// First create succeeds
		exporter.executeCreateDdl();
		// Second create fails because table already exists
		assertThrows(SchemaManagementException.class, exporter::executeCreateDdl);
	}

	@Test
	public void testNoHaltOnErrorSuppressesDuplicateCreate() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMP_NO_HALT", "EmpNoHalt", "com.example");
		Properties props = h2FileProperties();
		DdlExporter exporter = DdlExporter.create(List.of(entity), props)
				.haltOnError(false);
		// First create succeeds
		exporter.executeCreateDdl();
		// Second create should not throw even though table exists
		assertDoesNotThrow(exporter::executeCreateDdl);
	}
}
