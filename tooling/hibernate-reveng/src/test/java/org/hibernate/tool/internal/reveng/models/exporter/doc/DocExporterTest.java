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
package org.hibernate.tool.internal.reveng.models.exporter.doc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DocExporter}.
 *
 * @author Koen Aers
 */
public class DocExporterTest {

	private static final String OUTPUT_DIR = "./target/test-doc-export";
	private File outputDir;

	@BeforeEach
	public void setUp() {
		outputDir = new File(OUTPUT_DIR);
		deleteDir(outputDir);
		outputDir.mkdirs();
	}

	@AfterEach
	public void tearDown() {
		deleteDir(outputDir);
	}

	private void deleteDir(File dir) {
		if (dir.exists()) {
			try {
				Files.walk(dir.toPath())
						.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			}
			catch (IOException ignored) {
			}
		}
	}

	private ClassDetails buildEntity(DynamicEntityBuilder builder,
									  String tableName, String className,
									  String pkg) {
		TableMetadata table = new TableMetadata(tableName, className, pkg);
		table.addColumn(
				new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		return builder.createEntityFromTable(table);
	}

	// ---- Structure tests ----

	@Test
	public void testExportCreatesDirectoryStructure() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		assertTrue(new File(outputDir, "entities").isDirectory());
		assertTrue(new File(outputDir, "tables").isDirectory());
		assertTrue(new File(outputDir, "assets").isDirectory());
	}

	@Test
	public void testExportCreatesAssetFiles() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		assertTrue(new File(outputDir, "assets/doc-style.css").exists());
		assertTrue(new File(outputDir, "assets/hibernate_logo.gif").exists());
		assertTrue(new File(outputDir, "assets/inherit.gif").exists());
	}

	@Test
	public void testExportCreatesMainIndex() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		assertTrue(new File(outputDir, "index.html").exists());
	}

	// ---- Entity index and summary tests ----

	@Test
	public void testExportCreatesEntityIndex() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		File indexFile = new File(outputDir, "entities/index.html");
		assertTrue(indexFile.exists());
		String content = readFile(indexFile);
		assertTrue(content.contains("frameset"),
				"Entity index should use framesets");
	}

	@Test
	public void testExportCreatesEntitySummary() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		File summaryFile = new File(outputDir, "entities/summary.html");
		assertTrue(summaryFile.exists());
		String content = readFile(summaryFile);
		assertTrue(content.contains("com.example"),
				"Summary should contain package name");
	}

	// ---- Entity detail tests ----

	@Test
	public void testExportCreatesEntityDetailFile() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		File entityFile = new File(outputDir,
				"entities/com/example/Employee.html");
		assertTrue(entityFile.exists(),
				"Entity detail file should exist");
		String content = readFile(entityFile);
		assertTrue(content.contains("Employee"),
				"Entity page should contain entity name");
		assertTrue(content.contains("com.example"),
				"Entity page should contain package name");
	}

	@Test
	public void testEntityDetailContainsIdentifier() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		File entityFile = new File(outputDir,
				"entities/com/example/Employee.html");
		String content = readFile(entityFile);
		assertTrue(content.contains("Identifier Summary"),
				"Should have Identifier Summary section");
		assertTrue(content.contains("id"),
				"Should contain identifier name");
	}

	@Test
	public void testEntityDetailContainsProperties() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		File entityFile = new File(outputDir,
				"entities/com/example/Employee.html");
		String content = readFile(entityFile);
		assertTrue(content.contains("Property Summary"),
				"Should have Property Summary section");
		assertTrue(content.contains("name"),
				"Should contain property 'name'");
	}

	// ---- Multiple entity tests ----

	@Test
	public void testMultipleEntitiesSamePackage() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		ClassDetails department = buildEntity(
				builder, "DEPARTMENT", "Department", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(employee, department));
		exporter.export(outputDir);

		assertTrue(new File(outputDir,
				"entities/com/example/Employee.html").exists());
		assertTrue(new File(outputDir,
				"entities/com/example/Department.html").exists());

		// All entities list should contain both
		File allEntities = new File(outputDir, "entities/allentities.html");
		String content = readFile(allEntities);
		assertTrue(content.contains("Employee"));
		assertTrue(content.contains("Department"));
	}

	@Test
	public void testMultipleEntitiesDifferentPackages() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.hr");
		ClassDetails product = buildEntity(
				builder, "PRODUCT", "Product", "com.inventory");
		DocExporter exporter = DocExporter.create(
				List.of(employee, product));
		exporter.export(outputDir);

		assertTrue(new File(outputDir,
				"entities/com/hr/Employee.html").exists());
		assertTrue(new File(outputDir,
				"entities/com/inventory/Product.html").exists());

		// Package list should contain both packages
		File allPackages = new File(outputDir,
				"entities/allpackages.html");
		String content = readFile(allPackages);
		assertTrue(content.contains("com.hr"));
		assertTrue(content.contains("com.inventory"));
	}

	// ---- Package summary tests ----

	@Test
	public void testPackageSummaryGenerated() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		ClassDetails department = buildEntity(
				builder, "DEPARTMENT", "Department", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(employee, department));
		exporter.export(outputDir);

		File summaryFile = new File(outputDir,
				"entities/com/example/summary.html");
		assertTrue(summaryFile.exists(),
				"Package summary should exist");
		String content = readFile(summaryFile);
		assertTrue(content.contains("com.example"),
				"Should contain package name");
		assertTrue(content.contains("Employee"),
				"Should list Employee");
		assertTrue(content.contains("Department"),
				"Should list Department");
	}

	// ---- Per-package entity list tests ----

	@Test
	public void testPerPackageEntityList() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(List.of(employee));
		exporter.export(outputDir);

		File entityListFile = new File(outputDir,
				"entities/com/example/entities.html");
		assertTrue(entityListFile.exists(),
				"Per-package entity list should exist");
		String content = readFile(entityListFile);
		assertTrue(content.contains("Employee"),
				"Should list Employee");
	}

	private String readFile(File file) {
		try {
			return Files.readString(file.toPath());
		}
		catch (IOException e) {
			fail("Failed to read file: " + file);
			return "";
		}
	}
}
