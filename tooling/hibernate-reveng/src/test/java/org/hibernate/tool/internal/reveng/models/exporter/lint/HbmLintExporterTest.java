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
package org.hibernate.tool.internal.reveng.models.exporter.lint;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HbmLintExporter}.
 *
 * @author Koen Aers
 */
public class HbmLintExporterTest {

	private static final String OUTPUT_DIR = "./target/test-lint-export";
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

	// ---- ShadowedIdentifierDetector tests ----

	@Test
	public void testNoIssuesForCleanEntity() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		table.addColumn(
				new ColumnMetadata("EMP_ID", "empId", Long.class)
						.primaryKey(true));
		table.addColumn(
				new ColumnMetadata("NAME", "name", String.class));
		ClassDetails entity = builder.createEntityFromTable(table);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(entity));
		List<Issue> issues = exporter.analyze();

		assertTrue(issues.isEmpty(),
				"Clean entity should have no issues");
	}

	@Test
	public void testShadowedIdDetected() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		table.addColumn(
				new ColumnMetadata("EMP_ID", "empId", Long.class)
						.primaryKey(true));
		// Add a non-PK column named "id" — should trigger warning
		table.addColumn(
				new ColumnMetadata("ID", "id", String.class));
		ClassDetails entity = builder.createEntityFromTable(table);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(entity));
		List<Issue> issues = exporter.analyze();

		assertEquals(1, issues.size());
		assertEquals("ID_SHADOWED", issues.get(0).getType());
		assertTrue(issues.get(0).getDescription().contains("id"));
	}

	@Test
	public void testNoShadowedIdWhenIdIsIdentifier() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		table.addColumn(
				new ColumnMetadata("ID", "id", Long.class)
						.primaryKey(true));
		table.addColumn(
				new ColumnMetadata("NAME", "name", String.class));
		ClassDetails entity = builder.createEntityFromTable(table);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(entity));
		List<Issue> issues = exporter.analyze();

		assertTrue(issues.isEmpty(),
				"'id' as identifier should not be flagged");
	}

	// ---- BadCachingDetector tests ----

	@Test
	public void testCachedCollectionWithNonCacheableTarget() {
		ClassLoading classLoading =
				SimpleClassLoading.SIMPLE_CLASS_LOADING;
		ModelsContext ctx =
				new BasicModelsContextImpl(classLoading, false, null);

		// Create target entity WITHOUT @Cacheable
		DynamicClassDetails target = new DynamicClassDetails(
				"Department", "com.example.Department",
				Object.class, false, null, null, ctx);

		// Create source entity with @Cache on a collection field
		DynamicClassDetails source = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				Object.class, false, null, null, ctx);

		addCachedOneToManyField(source, target, "departments", ctx);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(source, target));
		List<Issue> issues = exporter.analyze();

		boolean found = issues.stream()
				.anyMatch(i -> "CACHE_COLLECTION_NONCACHABLE_TARGET"
						.equals(i.getType()));
		assertTrue(found,
				"Should detect cached collection with"
				+ " non-cacheable target");
	}

	@Test
	public void testCachedCollectionWithCacheableTarget() {
		ClassLoading classLoading =
				SimpleClassLoading.SIMPLE_CLASS_LOADING;
		ModelsContext ctx =
				new BasicModelsContextImpl(classLoading, false, null);

		// Create target entity WITH @Cacheable
		DynamicClassDetails target = new DynamicClassDetails(
				"Department", "com.example.Department",
				Object.class, false, null, null, ctx);
		var cacheableAnn =
				JpaAnnotations.CACHEABLE.createUsage(ctx);
		cacheableAnn.value(true);
		target.addAnnotationUsage(cacheableAnn);

		// Create source entity with @Cache on a collection field
		DynamicClassDetails source = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				Object.class, false, null, null, ctx);

		addCachedOneToManyField(source, target, "departments", ctx);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(source, target));
		List<Issue> issues = exporter.analyze();

		boolean found = issues.stream()
				.anyMatch(i -> "CACHE_COLLECTION_NONCACHABLE_TARGET"
						.equals(i.getType()));
		assertFalse(found,
				"Should not flag cached collection when target is"
				+ " cacheable");
	}

	private void addCachedOneToManyField(DynamicClassDetails owner,
										  DynamicClassDetails target,
										  String fieldName,
										  ModelsContext ctx) {
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(
				target, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType),
				null);
		DynamicFieldDetails field = owner.applyAttribute(
				fieldName, fieldType, false, true, ctx);

		var o2mAnnotation =
				JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		field.addAnnotationUsage(o2mAnnotation);

		CacheAnnotation cacheAnnotation = new CacheAnnotation(ctx);
		cacheAnnotation.usage(CacheConcurrencyStrategy.READ_WRITE);
		field.addAnnotationUsage(cacheAnnotation);
	}

	// ---- Export tests ----

	@Test
	public void testExportGeneratesReportFile() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		table.addColumn(
				new ColumnMetadata("EMP_ID", "empId", Long.class)
						.primaryKey(true));
		table.addColumn(
				new ColumnMetadata("NAME", "name", String.class));
		ClassDetails entity = builder.createEntityFromTable(table);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(entity));
		exporter.export(outputDir);

		File reportFile = new File(outputDir, "hbmlint-result.txt");
		assertTrue(reportFile.exists(),
				"Report file should exist");
	}

	@Test
	public void testExportReportContainsIssues() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		table.addColumn(
				new ColumnMetadata("EMP_ID", "empId", Long.class)
						.primaryKey(true));
		table.addColumn(
				new ColumnMetadata("ID", "id", String.class));
		ClassDetails entity = builder.createEntityFromTable(table);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(entity));
		exporter.export(outputDir);

		File reportFile = new File(outputDir, "hbmlint-result.txt");
		String content = readFile(reportFile);
		assertTrue(content.contains("ID_SHADOWED"),
				"Report should contain shadowed id issue");
	}

	@Test
	public void testExportEmptyReportForCleanEntity() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata table = new TableMetadata(
				"EMPLOYEE", "Employee", "com.example");
		table.addColumn(
				new ColumnMetadata("EMP_ID", "empId", Long.class)
						.primaryKey(true));
		table.addColumn(
				new ColumnMetadata("NAME", "name", String.class));
		ClassDetails entity = builder.createEntityFromTable(table);

		HbmLintExporter exporter =
				HbmLintExporter.create(List.of(entity));
		exporter.export(outputDir);

		File reportFile = new File(outputDir, "hbmlint-result.txt");
		String content = readFile(reportFile).trim();
		assertTrue(content.isEmpty(),
				"Report should be empty for clean entity");
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
