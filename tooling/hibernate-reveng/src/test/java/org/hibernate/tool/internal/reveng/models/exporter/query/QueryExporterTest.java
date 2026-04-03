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
package org.hibernate.tool.internal.reveng.models.exporter.query;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link QueryExporter}.
 *
 * @author Koen Aers
 */
public class QueryExporterTest {

	private static final String H2_URL =
			"jdbc:h2:mem:querytest;DB_CLOSE_DELAY=-1";

	private static final String EMPLOYEE_CLASS =
			Employee.class.getName();

	@BeforeEach
	public void setUp() throws SQLException {
		try (Connection conn = openConnection();
			 Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE EMPLOYEE ("
					+ "ID BIGINT PRIMARY KEY, "
					+ "NAME VARCHAR(255))");
			stmt.execute(
					"INSERT INTO EMPLOYEE VALUES (1, 'Alice')");
			stmt.execute(
					"INSERT INTO EMPLOYEE VALUES (2, 'Bob')");
			stmt.execute(
					"INSERT INTO EMPLOYEE VALUES (3, 'Charlie')");
		}
	}

	@AfterEach
	public void tearDown() throws SQLException {
		try (Connection conn = openConnection();
			 Statement stmt = conn.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS EMPLOYEE");
		}
	}

	private Connection openConnection() throws SQLException {
		return DriverManager.getConnection(H2_URL, "sa", "");
	}

	private Properties h2Properties() {
		Properties props = new Properties();
		props.put(AvailableSettings.URL, H2_URL);
		props.put(AvailableSettings.DRIVER, "org.h2.Driver");
		props.put(AvailableSettings.USER, "sa");
		props.put(AvailableSettings.PASS, "");
		props.put(AvailableSettings.DEFAULT_CATALOG, "");
		props.put(AvailableSettings.DEFAULT_SCHEMA, "");
		return props;
	}

	private ClassDetails employeeClassDetails() {
		ClassLoading classLoading =
				SimpleClassLoading.SIMPLE_CLASS_LOADING;
		ModelsContext ctx =
				new BasicModelsContextImpl(classLoading, false, null);
		return ctx.getClassDetailsRegistry()
				.resolveClassDetails(EMPLOYEE_CLASS);
	}

	@Test
	public void testExportToWriter() {
		ClassDetails entity = employeeClassDetails();
		QueryExporter exporter = QueryExporter.create(
				List.of(entity), h2Properties(),
				List.of("from " + EMPLOYEE_CLASS));
		StringWriter writer = new StringWriter();
		exporter.export(writer);
		String output = writer.toString();
		assertTrue(output.contains("Alice"),
				"Output should contain Alice");
		assertTrue(output.contains("Bob"),
				"Output should contain Bob");
		assertTrue(output.contains("Charlie"),
				"Output should contain Charlie");
	}

	@Test
	public void testExportToFile() throws IOException {
		ClassDetails entity = employeeClassDetails();
		File outputFile = new File(
				"./target/test-query-output/results.txt");
		QueryExporter exporter = QueryExporter.create(
				List.of(entity), h2Properties(),
				List.of("from " + EMPLOYEE_CLASS));
		exporter.export(outputFile);
		assertTrue(outputFile.exists(),
				"Output file should exist");
		String content = Files.readString(outputFile.toPath());
		assertTrue(content.contains("Alice"),
				"File should contain Alice");
		assertTrue(content.contains("Bob"),
				"File should contain Bob");
		outputFile.delete();
		outputFile.getParentFile().delete();
	}

	@Test
	public void testMultipleQueries() {
		ClassDetails entity = employeeClassDetails();
		QueryExporter exporter = QueryExporter.create(
				List.of(entity), h2Properties(),
				List.of(
						"from " + EMPLOYEE_CLASS + " e"
						+ " where e.name = 'Alice'",
						"from " + EMPLOYEE_CLASS + " e"
						+ " where e.name = 'Bob'"));
		StringWriter writer = new StringWriter();
		exporter.export(writer);
		String output = writer.toString();
		assertTrue(output.contains("Alice"),
				"Output should contain Alice");
		assertTrue(output.contains("Bob"),
				"Output should contain Bob");
		assertFalse(output.contains("Charlie"),
				"Output should not contain Charlie");
	}

	@Test
	public void testEmptyResult() {
		ClassDetails entity = employeeClassDetails();
		QueryExporter exporter = QueryExporter.create(
				List.of(entity), h2Properties(),
				List.of("from " + EMPLOYEE_CLASS + " e"
						+ " where e.name = 'Nobody'"));
		StringWriter writer = new StringWriter();
		exporter.export(writer);
		String output = writer.toString().trim();
		assertTrue(output.isEmpty(),
				"Output should be empty for no results");
	}

	@Test
	public void testEmptyQueryList() {
		ClassDetails entity = employeeClassDetails();
		QueryExporter exporter = QueryExporter.create(
				List.of(entity), h2Properties(),
				List.of());
		StringWriter writer = new StringWriter();
		exporter.export(writer);
		String output = writer.toString().trim();
		assertTrue(output.isEmpty(),
				"Output should be empty for no queries");
	}
}
