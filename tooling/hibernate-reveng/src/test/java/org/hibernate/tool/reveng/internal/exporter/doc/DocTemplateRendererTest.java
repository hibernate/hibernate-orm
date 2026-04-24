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
package org.hibernate.tool.reveng.internal.exporter.doc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocTemplateRendererTest {

	@TempDir
	Path tempDir;

	private DocTemplateRenderer renderer;

	@BeforeEach
	void setUp() {
		renderer = new DocTemplateRenderer(new String[0]);
	}

	@Test
	void testCopyResourceCopiesContent() {
		File target = tempDir.resolve("style.css").toFile();
		DocTemplateRenderer.copyResource(
				getClass().getClassLoader(),
				"doc/doc-style.css", target);
		assertTrue(target.exists(), "Target file should exist");
		assertTrue(target.length() > 0, "File should not be empty");
	}

	@Test
	void testCopyResourceThrowsForMissingResource() {
		File target = tempDir.resolve("missing.txt").toFile();
		assertThrows(IllegalArgumentException.class, () ->
				DocTemplateRenderer.copyResource(
						getClass().getClassLoader(),
						"nonexistent/resource.txt", target));
	}

	@Test
	void testReadFileContentReadsExistingFile() throws IOException {
		File file = tempDir.resolve("test.txt").toFile();
		Files.writeString(file.toPath(), "hello world\nsecond line\n");
		String content = DocTemplateRenderer.readFileContent(file);
		assertTrue(content.contains("hello world"));
		assertTrue(content.contains("second line"));
	}

	@Test
	void testReadFileContentReturnsEmptyForMissingFile() {
		File file = tempDir.resolve("nonexistent.txt").toFile();
		String content = DocTemplateRenderer.readFileContent(file);
		assertEquals("", content);
	}

	@Test
	void testGenerateDotReturnsFalseForNullExecutable() {
		EntityDocHelper docHelper = buildDocHelper();
		EntityDocFileManager docFileManager =
				new EntityDocFileManager(docHelper, tempDir.toFile());
		boolean result = renderer.generateDot(
				tempDir.toFile(), null, docHelper, docFileManager,
				"doc/dot/entitygraph.dot.ftl",
				"doc/dot/tablegraph.dot.ftl");
		assertFalse(result);
	}

	@Test
	void testGenerateDotReturnsFalseForEmptyExecutable() {
		EntityDocHelper docHelper = buildDocHelper();
		EntityDocFileManager docFileManager =
				new EntityDocFileManager(docHelper, tempDir.toFile());
		boolean result = renderer.generateDot(
				tempDir.toFile(), "", docHelper, docFileManager,
				"doc/dot/entitygraph.dot.ftl",
				"doc/dot/tablegraph.dot.ftl");
		assertFalse(result);
	}

	@Test
	void testGenerateDotCreatesBothDotFiles() {
		EntityDocHelper docHelper = buildDocHelper();
		EntityDocFileManager docFileManager =
				new EntityDocFileManager(docHelper, tempDir.toFile());
		renderer.generateDot(
				tempDir.toFile(), "/nonexistent/dot",
				docHelper, docFileManager,
				"doc/dot/entitygraph.dot.ftl",
				"doc/dot/tablegraph.dot.ftl");
		assertTrue(
				new File(tempDir.toFile(), "entities/entitygraph.dot")
						.exists(),
				"Entity DOT file should be created");
		assertTrue(
				new File(tempDir.toFile(), "tables/tablegraph.dot")
						.exists(),
				"Table DOT file should be created");
	}

	@Test
	void testProcessTemplateCreatesOutputFile() {
		EntityDocHelper docHelper = buildDocHelper();
		EntityDocFileManager docFileManager =
				new EntityDocFileManager(docHelper, tempDir.toFile());
		File entitiesDir = new File(tempDir.toFile(), "entities");
		entitiesDir.mkdirs();
		DocFile docFile = docFileManager.getClassIndexDocFile();
		renderer.processTemplate(
				Map.of("docFile", docFile),
				"doc/entities/index.ftl",
				docFile.getFile(), docHelper, docFileManager);
		assertTrue(docFile.getFile().exists(),
				"Template output file should be created");
		assertTrue(docFile.getFile().length() > 0,
				"Template output should not be empty");
	}

	@Test
	void testProcessTemplateThrowsForMissingTemplate() {
		EntityDocHelper docHelper = buildDocHelper();
		EntityDocFileManager docFileManager =
				new EntityDocFileManager(docHelper, tempDir.toFile());
		File outputFile = tempDir.resolve("out.html").toFile();
		assertThrows(RuntimeException.class, () ->
				renderer.processTemplate(
						Map.of(), "nonexistent/template.ftl",
						outputFile, docHelper, docFileManager));
	}

	@Test
	void testConstructorWithFileTemplatePath() throws IOException {
		Path customTemplates = tempDir.resolve("templates");
		Files.createDirectories(customTemplates);
		DocTemplateRenderer custom = new DocTemplateRenderer(
				new String[]{customTemplates.toString()});
		assertNotNull(custom);
	}

	@Test
	void testConstructorWithNullTemplatePath() {
		DocTemplateRenderer custom = new DocTemplateRenderer(null);
		assertNotNull(custom);
	}

	private EntityDocHelper buildDocHelper() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor table = new TableDescriptor(
				"PERSON", "Person", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		builder.createEntityFromTable(table);
		List<ClassDetails> entities = List.of(
				builder.getModelsContext()
						.getClassDetailsRegistry()
						.getClassDetails("com.example.Person"));
		return new EntityDocHelper(
				entities, builder.getTableDescriptorMap());
	}
}
