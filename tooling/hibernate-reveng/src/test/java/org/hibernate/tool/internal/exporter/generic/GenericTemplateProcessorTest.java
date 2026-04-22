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
package org.hibernate.tool.internal.exporter.generic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.version.Version;
import org.hibernate.tool.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenericTemplateProcessorTest {

	private static final String TEMPLATE_BASE =
			"org/hibernate/tool/internal/reveng/models/exporter/generic/";

	@TempDir
	Path tempDir;

	// --- resolveModes ---

	@Test
	void testResolveModesWithClassNamePattern() {
		List<String> modes = GenericTemplateProcessor.resolveModes(
				null, "{package-name}/{class-name}.java");
		assertEquals(List.of("entity", "component"), modes);
	}

	@Test
	void testResolveModesWithoutClassNamePattern() {
		List<String> modes = GenericTemplateProcessor.resolveModes(
				null, "output.txt");
		assertEquals(List.of("configuration"), modes);
	}

	@Test
	void testResolveModesWithExplicitForEach() {
		List<String> modes = GenericTemplateProcessor.resolveModes(
				"entity", "{class-name}.txt");
		assertEquals(List.of("entity"), modes);
	}

	@Test
	void testResolveModesWithMultipleForEach() {
		List<String> modes = GenericTemplateProcessor.resolveModes(
				"entity,component,configuration",
				"{class-name}.txt");
		assertEquals(
				List.of("entity", "component", "configuration"),
				modes);
	}

	@Test
	void testResolveModesTrimsWhitespace() {
		List<String> modes = GenericTemplateProcessor.resolveModes(
				" entity , component ", "{class-name}.txt");
		assertEquals(List.of("entity", "component"), modes);
	}

	@Test
	void testResolveModesEmptyForEachInfersFromPattern() {
		List<String> modes = GenericTemplateProcessor.resolveModes(
				"", "summary.txt");
		assertEquals(List.of("configuration"), modes);
	}

	// --- resolveOutputClassName ---

	@Test
	void testResolveOutputClassNameWithoutMetadata() {
		ClassDetails entity = buildEntity(
				"PERSON", "Person", "com.example");
		String result = GenericTemplateProcessor
				.resolveOutputClassName(entity, null);
		assertEquals("com.example.Person", result);
	}

	// --- resolveFilename ---

	@Test
	void testResolveFilenameWithPackageAndClass() {
		ClassDetails entity = buildEntity(
				"PERSON", "Person", "com.example");
		String result = GenericTemplateProcessor.resolveFilename(
				entity, "{package-name}/{class-name}.java", null);
		assertEquals("com/example/Person.java", result);
	}

	@Test
	void testResolveFilenameWithoutPackage() {
		ClassDetails entity = buildEntity(
				"ITEM", "Item", null);
		String result = GenericTemplateProcessor.resolveFilename(
				entity, "{package-name}/{class-name}.txt", null);
		assertEquals("./{class-name}.txt".replace(
				"{class-name}", "Item"), result);
	}

	@Test
	void testResolveFilenameClassNameOnly() {
		ClassDetails entity = buildEntity(
				"PERSON", "Person", "com.example");
		String result = GenericTemplateProcessor.resolveFilename(
				entity, "{class-name}.xml", null);
		assertEquals("Person.xml", result);
	}

	// --- processTemplate ---

	@Test
	void testProcessTemplateRendersOutput() {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "config-template.ftl",
						new String[0]);
		List<ClassDetails> entities = buildEntities();
		Map<String, Object> model =
				processor.buildModel(new Properties(), null);
		model.put("entities", entities);
		StringWriter writer = new StringWriter();
		processor.processTemplate(model, writer);
		String content = writer.toString();
		assertTrue(content.contains("entityCount=2"), content);
	}

	@Test
	void testProcessTemplateThrowsForMissingTemplate() {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						"nonexistent/template.ftl",
						new String[0]);
		Map<String, Object> model =
				processor.buildModel(new Properties(), null);
		assertThrows(RuntimeException.class, () ->
				processor.processTemplate(
						model, new StringWriter()));
	}

	// --- buildModel ---

	@Test
	void testBuildModelContainsDateAndVersion() {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "config-template.ftl",
						new String[0]);
		Map<String, Object> model =
				processor.buildModel(new Properties(), null);
		assertNotNull(model.get("date"));
		assertEquals(Version.versionString(), model.get("version"));
		assertSame(model, model.get("ctx"));
	}

	@Test
	void testBuildModelTransformsBooleanStrings() {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "config-template.ftl",
						new String[0]);
		Properties props = new Properties();
		props.put("myFlag", "true");
		props.put("otherFlag", "false");
		props.put("textProp", "hello");
		Map<String, Object> model =
				processor.buildModel(props, null);
		assertEquals(Boolean.TRUE, model.get("myFlag"));
		assertEquals(Boolean.FALSE, model.get("otherFlag"));
		assertEquals("hello", model.get("textProp"));
	}

	@Test
	void testBuildModelStripsHibernatetoolPrefix() {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "config-template.ftl",
						new String[0]);
		Properties props = new Properties();
		props.put("hibernatetool.mykey", "myvalue");
		Map<String, Object> model =
				processor.buildModel(props, null);
		assertEquals("myvalue", model.get("hibernatetool.mykey"));
		assertEquals("myvalue", model.get("mykey"));
	}

	// --- exportPerClass ---

	@Test
	void testExportPerClassCreatesFiles() throws IOException {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "entity-template.ftl",
						new String[0]);
		List<ClassDetails> entities = buildEntities();
		List<String> exported = new ArrayList<>();
		processor.exportPerClass(
				tempDir.toFile(),
				"{package-name}/{class-name}.txt",
				entities, "Entity", null,
				(writer, entity) -> {
					try {
						writer.write("exported:"
								+ entity.getClassName());
						exported.add(entity.getClassName());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
		assertEquals(2, exported.size());
		assertTrue(Files.exists(
				tempDir.resolve("com/example/Author.txt")));
		assertTrue(Files.exists(
				tempDir.resolve("com/example/Article.txt")));
		String content = Files.readString(
				tempDir.resolve("com/example/Author.txt"));
		assertEquals("exported:com.example.Author", content);
	}

	@Test
	void testExportPerClassWrapsExceptions() {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "entity-template.ftl",
						new String[0]);
		List<ClassDetails> entities = buildEntities();
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> processor.exportPerClass(
						tempDir.toFile(),
						"{class-name}.txt",
						entities, "Entity", null,
						(writer, entity) -> {
							throw new RuntimeException("boom");
						}));
		assertTrue(ex.getMessage().contains("Entity"),
				ex.getMessage());
	}

	// --- exportConfiguration ---

	@Test
	void testExportConfigurationCreatesFile() throws IOException {
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "config-template.ftl",
						new String[0]);
		List<ClassDetails> entities = buildEntities();
		processor.exportConfiguration(
				tempDir.toFile(), "output.txt",
				new Properties(), entities, null);
		Path outputFile = tempDir.resolve("output.txt");
		assertTrue(Files.exists(outputFile));
		String content = Files.readString(outputFile);
		assertTrue(content.contains("entityCount=2"), content);
	}

	// --- Constructor with custom template path ---

	@Test
	void testConstructorWithCustomTemplatePath() throws IOException {
		Path customDir = tempDir.resolve("templates");
		Files.createDirectories(customDir);
		GenericTemplateProcessor processor =
				new GenericTemplateProcessor(
						TEMPLATE_BASE + "config-template.ftl",
						new String[]{customDir.toString()});
		assertNotNull(processor);
	}

	// --- Helpers ---

	private ClassDetails buildEntity(
			String tableName, String entityName,
			String packageName) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor table = new TableDescriptor(
				tableName, entityName, packageName);
		table.addColumn(new ColumnDescriptor(
				"ID", "id", Long.class).primaryKey(true));
		builder.createEntityFromTable(table);
		String fqn = packageName != null
				? packageName + "." + entityName : entityName;
		return builder.getModelsContext()
				.getClassDetailsRegistry()
				.getClassDetails(fqn);
	}

	private List<ClassDetails> buildEntities() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor t1 = new TableDescriptor(
				"AUTHOR", "Author", "com.example");
		t1.addColumn(new ColumnDescriptor(
				"ID", "id", Long.class).primaryKey(true));
		TableDescriptor t2 = new TableDescriptor(
				"ARTICLE", "Article", "com.example");
		t2.addColumn(new ColumnDescriptor(
				"ID", "id", Long.class).primaryKey(true));
		ClassDetails author = builder.createEntityFromTable(t1);
		ClassDetails article = builder.createEntityFromTable(t2);
		return List.of(author, article);
	}
}
