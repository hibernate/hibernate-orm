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
package org.hibernate.tool.internal.reveng.models.exporter.generic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.builder.db.EmbeddableClassBuilder;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.EmbeddableDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link GenericExporter}.
 *
 * @author Koen Aers
 */
public class GenericExporterTest {

	private static final String TEMPLATE_BASE =
			"org/hibernate/tool/internal/reveng/models/exporter/generic/";

	@TempDir
	File outputDir;

	private List<ClassDetails> createEntities() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor authorTable = new TableDescriptor("AUTHOR", "Author", "com.example");
		authorTable.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		authorTable.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TableDescriptor articleTable = new TableDescriptor("ARTICLE", "Article", "com.example");
		articleTable.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		articleTable.addColumn(new ColumnDescriptor("TITLE", "title", String.class));
		ClassDetails author = builder.createEntityFromTable(authorTable);
		ClassDetails article = builder.createEntityFromTable(articleTable);
		return List.of(author, article);
	}

	private List<ClassDetails> createEntitiesAndEmbeddable() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor personTable = new TableDescriptor("PERSON", "Person", "com.example");
		personTable.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		personTable.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		ClassDetails person = builder.createEntityFromTable(personTable);
		EmbeddableDescriptor addressMeta = new EmbeddableDescriptor("Address", "com.example");
		addressMeta.addColumn(new ColumnDescriptor("STREET", "street", String.class));
		addressMeta.addColumn(new ColumnDescriptor("CITY", "city", String.class));
		ClassDetails address = EmbeddableClassBuilder.buildEmbeddableClass(
				addressMeta, builder.getModelsContext());
		return List.of(person, address);
	}

	// --- Entity mode tests ---

	@Test
	public void testPerEntityFileGeneration() throws IOException {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				TEMPLATE_BASE + "entity-template.ftl",
				"{package-name}/{class-name}.txt");
		exporter.exportAll(outputDir);
		File authorFile = new File(outputDir, "com/example/Author.txt");
		File articleFile = new File(outputDir, "com/example/Article.txt");
		assertTrue(authorFile.exists(), "Author.txt should exist");
		assertTrue(articleFile.exists(), "Article.txt should exist");
		String authorContent = Files.readString(authorFile.toPath());
		assertTrue(authorContent.contains("className=Author"), authorContent);
		assertTrue(authorContent.contains("packageName=com.example"), authorContent);
		assertTrue(authorContent.contains("entityClass=com.example.Author"), authorContent);
	}

	@Test
	public void testPerEntityWithoutPackage() throws IOException {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor table = new TableDescriptor("ITEM", "Item", null);
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		ClassDetails entity = builder.createEntityFromTable(table);
		GenericExporter exporter = GenericExporter.create(
				List.of(entity),
				TEMPLATE_BASE + "entity-template.ftl",
				"{package-name}/{class-name}.txt");
		exporter.exportAll(outputDir);
		File itemFile = new File(outputDir, "./Item.txt");
		assertTrue(itemFile.exists(), "Item.txt should exist");
	}

	@Test
	public void testFilePatternWithoutPackage() throws IOException {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				TEMPLATE_BASE + "entity-template.ftl",
				"generic{class-name}.txt");
		exporter.exportAll(outputDir);
		File authorFile = new File(outputDir, "genericAuthor.txt");
		File articleFile = new File(outputDir, "genericArticle.txt");
		assertTrue(authorFile.exists(), "genericAuthor.txt should exist");
		assertTrue(articleFile.exists(), "genericArticle.txt should exist");
	}

	// --- Configuration mode tests ---

	@Test
	public void testConfigurationMode() throws IOException {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				TEMPLATE_BASE + "config-template.ftl",
				"output.txt");
		exporter.exportAll(outputDir);
		File outputFile = new File(outputDir, "output.txt");
		assertTrue(outputFile.exists(), "output.txt should exist");
		String content = Files.readString(outputFile.toPath());
		assertTrue(content.contains("entityCount=2"), content);
		assertTrue(content.contains("entity=com.example.Author"), content);
		assertTrue(content.contains("entity=com.example.Article"), content);
	}

	@Test
	public void testConfigurationModeInferredFromFilePattern() throws IOException {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				TEMPLATE_BASE + "config-template.ftl",
				"summary.txt",
				null);
		exporter.exportAll(outputDir);
		assertTrue(new File(outputDir, "summary.txt").exists());
	}

	// --- ForEach tests ---

	@Test
	public void testForEachEntity() throws IOException {
		List<ClassDetails> mixed = createEntitiesAndEmbeddable();
		GenericExporter exporter = GenericExporter.create(
				mixed,
				TEMPLATE_BASE + "entity-template.ftl",
				"{package-name}/{class-name}.txt",
				"entity");
		exporter.exportAll(outputDir);
		assertTrue(
				new File(outputDir, "com/example/Person.txt").exists(),
				"Entity Person should be exported");
		assertFalse(
				new File(outputDir, "com/example/Address.txt").exists(),
				"Embeddable Address should not be exported in entity mode");
	}

	@Test
	public void testForEachComponent() throws IOException {
		List<ClassDetails> mixed = createEntitiesAndEmbeddable();
		GenericExporter exporter = GenericExporter.create(
				mixed,
				TEMPLATE_BASE + "entity-template.ftl",
				"{package-name}/{class-name}.txt",
				"component");
		exporter.exportAll(outputDir);
		assertFalse(
				new File(outputDir, "com/example/Person.txt").exists(),
				"Entity Person should not be exported in component mode");
		assertTrue(
				new File(outputDir, "com/example/Address.txt").exists(),
				"Embeddable Address should be exported in component mode");
	}

	@Test
	public void testForEachEntityAndComponent() throws IOException {
		List<ClassDetails> mixed = createEntitiesAndEmbeddable();
		GenericExporter exporter = GenericExporter.create(
				mixed,
				TEMPLATE_BASE + "entity-template.ftl",
				"{package-name}/{class-name}.txt",
				"entity,component");
		exporter.exportAll(outputDir);
		assertTrue(new File(outputDir, "com/example/Person.txt").exists());
		assertTrue(new File(outputDir, "com/example/Address.txt").exists());
	}

	@Test
	public void testForEachConfiguration() throws IOException {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				TEMPLATE_BASE + "config-template.ftl",
				"cfg-output.txt",
				"configuration");
		exporter.exportAll(outputDir);
		File outputFile = new File(outputDir, "cfg-output.txt");
		assertTrue(outputFile.exists());
		String content = Files.readString(outputFile.toPath());
		assertTrue(content.contains("entityCount=2"), content);
	}

	@Test
	public void testForEachInvalidValue() {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				TEMPLATE_BASE + "entity-template.ftl",
				"{class-name}.txt",
				"does_not_exist");
		assertThrows(RuntimeException.class, () -> exporter.exportAll(outputDir));
	}

	// --- Writer-based export ---

	@Test
	public void testExportToWriter() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		ClassDetails entity = builder.createEntityFromTable(table);
		GenericExporter exporter = GenericExporter.create(
				List.of(entity),
				TEMPLATE_BASE + "entity-template.ftl",
				"{class-name}.txt");
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String content = writer.toString();
		assertTrue(content.contains("className=Employee"), content);
		assertTrue(content.contains("packageName=com.example"), content);
		assertTrue(content.contains("entityClass=com.example.Employee"), content);
	}

	// --- Inferred mode from file pattern ---

	@Test
	public void testFilePatternWithClassNameInfersEntityMode() throws IOException {
		List<ClassDetails> mixed = createEntitiesAndEmbeddable();
		GenericExporter exporter = GenericExporter.create(
				mixed,
				TEMPLATE_BASE + "entity-template.ftl",
				"{class-name}.txt");
		exporter.exportAll(outputDir);
		// With {class-name} and no forEach, both entity and component run
		assertTrue(new File(outputDir, "Person.txt").exists());
		assertTrue(new File(outputDir, "Address.txt").exists());
	}

	// --- Validation tests ---

	@Test
	public void testMissingTemplateNameThrows() {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities, null, "output.txt");
		assertThrows(RuntimeException.class, () -> exporter.exportAll(outputDir));
	}

	@Test
	public void testMissingFilePatternThrows() {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				TEMPLATE_BASE + "entity-template.ftl",
				null);
		assertThrows(RuntimeException.class, () -> exporter.exportAll(outputDir));
	}

	// --- Template error propagation ---

	@Test
	public void testTemplateErrorPropagated() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor table = new TableDescriptor("ITEM", "Item", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		ClassDetails entity = builder.createEntityFromTable(table);
		GenericExporter exporter = GenericExporter.create(
				List.of(entity),
				TEMPLATE_BASE + "error-template.ftl",
				"{class-name}.txt",
				"entity");
		assertThrows(RuntimeException.class, () -> exporter.exportAll(outputDir));
	}

	// --- Accessors ---

	@Test
	public void testGetters() {
		List<ClassDetails> entities = createEntities();
		GenericExporter exporter = GenericExporter.create(
				entities,
				"my-template.ftl",
				"{class-name}.java",
				"entity");
		assertEquals("my-template.ftl", exporter.getTemplateName());
		assertEquals("{class-name}.java", exporter.getFilePattern());
		assertEquals("entity", exporter.getForEach());
		assertEquals(2, exporter.getEntities().size());
	}
}
