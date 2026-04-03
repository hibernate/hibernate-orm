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

	// ---- Table documentation tests ----

	@Test
	public void testExportCreatesTableIndex() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(entity), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File indexFile = new File(outputDir, "tables/index.html");
		assertTrue(indexFile.exists());
		String content = readFile(indexFile);
		assertTrue(content.contains("frameset"),
				"Table index should use framesets");
	}

	@Test
	public void testExportCreatesTableSummary() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(entity), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File summaryFile = new File(outputDir, "tables/summary.html");
		assertTrue(summaryFile.exists());
		String content = readFile(summaryFile);
		assertTrue(content.contains("default"),
				"Summary should contain default schema");
	}

	@Test
	public void testExportCreatesTableDetailFile() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(entity), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File tableFile = new File(outputDir, "tables/EMPLOYEE.html");
		assertTrue(tableFile.exists(),
				"Table detail file should exist");
		String content = readFile(tableFile);
		assertTrue(content.contains("EMPLOYEE"),
				"Table page should contain table name");
	}

	@Test
	public void testTableDetailContainsColumns() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(entity), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File tableFile = new File(outputDir, "tables/EMPLOYEE.html");
		String content = readFile(tableFile);
		assertTrue(content.contains("ID"),
				"Should contain column ID");
		assertTrue(content.contains("NAME"),
				"Should contain column NAME");
	}

	@Test
	public void testTableDetailContainsPrimaryKey() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(entity), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File tableFile = new File(outputDir, "tables/EMPLOYEE.html");
		String content = readFile(tableFile);
		assertTrue(content.contains("Primary Key"),
				"Should have Primary Key section");
		assertTrue(content.contains("PK_EMPLOYEE"),
				"Should contain primary key name");
	}

	@Test
	public void testMultipleTablesGenerated() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		ClassDetails department = buildEntity(
				builder, "DEPARTMENT", "Department", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(employee, department),
				builder.getTableMetadataMap());
		exporter.export(outputDir);

		assertTrue(new File(outputDir, "tables/EMPLOYEE.html").exists());
		assertTrue(new File(outputDir, "tables/DEPARTMENT.html").exists());

		// All tables list should contain both
		File allTables = new File(outputDir, "tables/alltables.html");
		String content = readFile(allTables);
		assertTrue(content.contains("EMPLOYEE"));
		assertTrue(content.contains("DEPARTMENT"));
	}

	@Test
	public void testTableWithForeignKey() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		// Create department first (target)
		TableMetadata deptTable = new TableMetadata(
				"DEPARTMENT", "Department", "com.example");
		deptTable.addColumn(
				new ColumnMetadata("DEPT_ID", "id", Long.class)
						.primaryKey(true));
		deptTable.addColumn(
				new ColumnMetadata("DEPT_NAME", "name", String.class));
		builder.createEntityFromTable(deptTable);

		// Create employee with FK to department
		TableMetadata empTable = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		empTable.addColumn(
				new ColumnMetadata("EMP_ID", "id", Long.class)
						.primaryKey(true));
		empTable.addColumn(
				new ColumnMetadata("EMP_NAME", "name", String.class));
		empTable.addForeignKey(new org.hibernate.tool.internal.reveng.models
				.metadata.ForeignKeyMetadata(
				"department", "DEPT_ID",
				"Department", "com.example"));
		ClassDetails dept = builder.getModelsContext()
				.getClassDetailsRegistry()
				.getClassDetails("com.example.Department");
		ClassDetails emp = builder.createEntityFromTable(empTable);

		DocExporter exporter = DocExporter.create(
				List.of(dept, emp), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File tableFile = new File(outputDir, "tables/EMPLOYEE.html");
		String content = readFile(tableFile);
		assertTrue(content.contains("Foreign Key"),
				"Should have Foreign Keys section");
		assertTrue(content.contains("DEPARTMENT"),
				"Should reference DEPARTMENT table");
	}

	@Test
	public void testTableWithSchema() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.addColumn(
				new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(
				new ColumnMetadata("NAME", "name", String.class));
		ClassDetails entity = builder.createEntityFromTable(table);

		DocExporter exporter = DocExporter.create(
				List.of(entity), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File tableFile = new File(outputDir, "tables/HR/EMPLOYEE.html");
		assertTrue(tableFile.exists(),
				"Table file should be in schema folder");
		String content = readFile(tableFile);
		assertTrue(content.contains("HR"),
				"Should contain schema name");

		// Schema summary should exist
		File schemaSummary = new File(outputDir,
				"tables/HR/summary.html");
		assertTrue(schemaSummary.exists(),
				"Schema summary should exist");
	}

	@Test
	public void testTableFallbackFromClassDetails() {
		// Create entity without using DynamicEntityBuilder (no TableMetadata)
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");

		// Pass null for tableMetadataMap to force ClassDetails fallback
		DocExporter exporter = DocExporter.create(List.of(entity));
		exporter.export(outputDir);

		// Table should still be generated from ClassDetails annotations
		File tableFile = new File(outputDir, "tables/EMPLOYEE.html");
		assertTrue(tableFile.exists(),
				"Table file should exist from ClassDetails fallback");
		String content = readFile(tableFile);
		assertTrue(content.contains("EMPLOYEE"),
				"Should contain table name from @Table annotation");
	}

	// ---- DOT graph tests ----

	@Test
	public void testDotEntityGraphGenerated() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		ClassDetails department = buildEntity(
				builder, "DEPARTMENT", "Department", "com.example");

		// Use a non-existent dot executable — DOT files should still
		// be generated even though dot conversion will fail
		DocExporter exporter = DocExporter.create(
				List.of(employee, department),
				builder.getTableMetadataMap(),
				"/nonexistent/dot",
				new String[0]);
		exporter.export(outputDir);

		File entityDot = new File(outputDir,
				"entities/entitygraph.dot");
		assertTrue(entityDot.exists(),
				"Entity graph DOT file should be generated");
		String content = readFile(entityDot);
		assertTrue(content.contains("digraph EntityGraph"),
				"Should be a valid DOT digraph");
		assertTrue(content.contains("Employee"),
				"Should contain Employee entity");
		assertTrue(content.contains("Department"),
				"Should contain Department entity");
	}

	@Test
	public void testDotTableGraphGenerated() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails employee = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		ClassDetails department = buildEntity(
				builder, "DEPARTMENT", "Department", "com.example");

		DocExporter exporter = DocExporter.create(
				List.of(employee, department),
				builder.getTableMetadataMap(),
				"/nonexistent/dot",
				new String[0]);
		exporter.export(outputDir);

		File tableDot = new File(outputDir,
				"tables/tablegraph.dot");
		assertTrue(tableDot.exists(),
				"Table graph DOT file should be generated");
		String content = readFile(tableDot);
		assertTrue(content.contains("digraph TableGraph"),
				"Should be a valid DOT digraph");
		assertTrue(content.contains("EMPLOYEE"),
				"Should contain EMPLOYEE table");
		assertTrue(content.contains("DEPARTMENT"),
				"Should contain DEPARTMENT table");
	}

	@Test
	public void testDotEntityGraphContainsSubclassEdge() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata parentTable = new TableMetadata(
				"PERSON", "Person", "com.example");
		parentTable.addColumn(
				new ColumnMetadata("ID", "id", Long.class)
						.primaryKey(true));
		parentTable.addColumn(
				new ColumnMetadata("NAME", "name", String.class));
		ClassDetails parent = builder.createEntityFromTable(parentTable);

		TableMetadata childTable = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		childTable.addColumn(
				new ColumnMetadata("ID", "id", Long.class)
						.primaryKey(true));
		childTable.addColumn(
				new ColumnMetadata("SALARY", "salary", Double.class));
		childTable.parent("Person", "com.example");
		ClassDetails child = builder.createEntityFromTable(childTable);

		DocExporter exporter = DocExporter.create(
				List.of(parent, child),
				builder.getTableMetadataMap(),
				"/nonexistent/dot",
				new String[0]);
		exporter.export(outputDir);

		File entityDot = new File(outputDir,
				"entities/entitygraph.dot");
		String content = readFile(entityDot);
		assertTrue(content.contains("arrowhead=\"onormal\""),
				"Should contain subclass edge");
	}

	@Test
	public void testDotTableGraphContainsForeignKeyEdge() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata deptTable = new TableMetadata(
				"DEPARTMENT", "Department", "com.example");
		deptTable.addColumn(
				new ColumnMetadata("DEPT_ID", "id", Long.class)
						.primaryKey(true));
		builder.createEntityFromTable(deptTable);

		TableMetadata empTable = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		empTable.addColumn(
				new ColumnMetadata("EMP_ID", "id", Long.class)
						.primaryKey(true));
		empTable.addForeignKey(new org.hibernate.tool.internal.reveng.models
				.metadata.ForeignKeyMetadata(
				"department", "DEPT_ID",
				"Department", "com.example"));
		ClassDetails dept = builder.getModelsContext()
				.getClassDetailsRegistry()
				.getClassDetails("com.example.Department");
		ClassDetails emp = builder.createEntityFromTable(empTable);

		DocExporter exporter = DocExporter.create(
				List.of(dept, emp),
				builder.getTableMetadataMap(),
				"/nonexistent/dot",
				new String[0]);
		exporter.export(outputDir);

		File tableDot = new File(outputDir,
				"tables/tablegraph.dot");
		String content = readFile(tableDot);
		assertTrue(content.contains("DEPARTMENT"),
				"Should contain FK edge to DEPARTMENT");
		assertTrue(content.contains("FK_"),
				"Should contain FK label");
	}

	@Test
	public void testNoDotGenerationWithoutExecutable() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = buildEntity(
				builder, "EMPLOYEE", "Employee", "com.example");
		DocExporter exporter = DocExporter.create(
				List.of(entity), builder.getTableMetadataMap());
		exporter.export(outputDir);

		File entityDot = new File(outputDir,
				"entities/entitygraph.dot");
		assertFalse(entityDot.exists(),
				"DOT file should not be generated without executable");
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
