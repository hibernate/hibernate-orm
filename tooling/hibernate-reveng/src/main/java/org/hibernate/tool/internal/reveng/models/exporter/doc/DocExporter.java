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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.export.doc.DocFile;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Generates HTML documentation for entities from a list of
 * {@link ClassDetails}. Uses the existing FreeMarker templates in
 * {@code doc/entities/*.ftl} with adapter objects that present
 * {@code ClassDetails}/{@code FieldDetails} in a shape compatible
 * with the template expectations.
 * <p>
 * This is the entity-side replacement for the old
 * {@code org.hibernate.tool.internal.export.doc.DocExporter}.
 * Table-side documentation can be added separately.
 *
 * @author Koen Aers
 */
public class DocExporter {

	private static final String FTL_ENTITIES_INDEX =
			"doc/entities/index.ftl";
	private static final String FTL_ENTITIES_SUMMARY =
			"doc/entities/summary.ftl";
	private static final String FTL_ENTITIES_ENTITY =
			"doc/entities/entity.ftl";
	private static final String FTL_ENTITIES_PACKAGE_LIST =
			"doc/entities/package-list.ftl";
	private static final String FTL_ENTITIES_ENTITY_LIST =
			"doc/entities/allEntity-list.ftl";
	private static final String FTL_ENTITIES_PERPACKAGE_ENTITY_LIST =
			"doc/entities/perPackageEntity-list.ftl";
	private static final String FTL_ENTITIES_PACKAGE_SUMMARY =
			"doc/entities/package-summary.ftl";

	private static final String FTL_TABLES_INDEX =
			"doc/tables/index.ftl";
	private static final String FTL_TABLES_SUMMARY =
			"doc/tables/summary.ftl";
	private static final String FTL_TABLES_TABLE =
			"doc/tables/table.ftl";
	private static final String FTL_TABLES_SCHEMA_LIST =
			"doc/tables/schema-list.ftl";
	private static final String FTL_TABLES_TABLE_LIST =
			"doc/tables/table-list.ftl";
	private static final String FTL_TABLES_SCHEMA_TABLE_LIST =
			"doc/tables/schema-table-list.ftl";
	private static final String FTL_TABLES_SCHEMA_SUMMARY =
			"doc/tables/schema-summary.ftl";

	private static final String FTL_DOT_ENTITY_GRAPH =
			"doc/dot/entitygraph.dot.ftl";
	private static final String FTL_DOT_TABLE_GRAPH =
			"doc/dot/tablegraph.dot.ftl";

	private static final String RES_CSS = "doc/doc-style.css";
	private static final String RES_HIBERNATE_IMAGE =
			"doc/hibernate_logo.gif";
	private static final String RES_EXTENDS_IMAGE = "doc/inherit.gif";
	private static final String RES_MAIN_INDEX = "doc/index.html";

	private static final Logger log =
			Logger.getLogger(DocExporter.class.getName());

	private final List<ClassDetails> entities;
	private final Map<String, TableMetadata> tableMetadataMap;
	private final String dotExecutable;
	private final Configuration freemarkerConfig;
	private final BeansWrapper beansWrapper;

	private DocExporter(List<ClassDetails> entities,
						Map<String, TableMetadata> tableMetadataMap,
						String dotExecutable,
						String[] templatePath) {
		this.entities = entities;
		this.tableMetadataMap = tableMetadataMap;
		this.dotExecutable = dotExecutable;
		this.beansWrapper = new BeansWrapperBuilder(
				Configuration.VERSION_2_3_33).build();
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(
				createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
		// Use BeansWrapper so templates can access bean properties
		// like list.empty (calls isEmpty()) on Java objects
		this.freemarkerConfig.setObjectWrapper(beansWrapper);
	}

	public static DocExporter create(List<ClassDetails> entities) {
		return new DocExporter(entities, null, null, new String[0]);
	}

	public static DocExporter create(List<ClassDetails> entities,
									  String[] templatePath) {
		return new DocExporter(entities, null, null, templatePath);
	}

	public static DocExporter create(List<ClassDetails> entities,
									  Map<String, TableMetadata> tableMetadataMap) {
		return new DocExporter(entities, tableMetadataMap, null,
				new String[0]);
	}

	public static DocExporter create(List<ClassDetails> entities,
									  Map<String, TableMetadata> tableMetadataMap,
									  String[] templatePath) {
		return new DocExporter(entities, tableMetadataMap, null,
				templatePath);
	}

	public static DocExporter create(List<ClassDetails> entities,
									  Map<String, TableMetadata> tableMetadataMap,
									  String dotExecutable,
									  String[] templatePath) {
		return new DocExporter(entities, tableMetadataMap, dotExecutable,
				templatePath);
	}

	/**
	 * Exports entity documentation to the given output directory.
	 * Creates the full folder structure with HTML files for each entity,
	 * package summaries, index pages, and supporting assets.
	 */
	public void export(File outputDirectory) {
		EntityDocHelper docHelper =
				new EntityDocHelper(entities, tableMetadataMap);
		EntityDocFileManager docFileManager =
				new EntityDocFileManager(docHelper, outputDirectory);

		copyAssets(docFileManager);
		copyMainIndex(docFileManager);

		boolean graphsGenerated = generateDot(
				outputDirectory, docHelper, docFileManager);

		generateEntitiesIndex(docHelper, docFileManager);
		generatePackageSummary(docHelper, docFileManager,
				graphsGenerated, outputDirectory);
		generateEntitiesDetails(docHelper, docFileManager);
		generateAllPackagesList(docHelper, docFileManager);
		generateAllEntitiesList(docHelper, docFileManager);
		generatePerPackageEntityList(docHelper, docFileManager);
		generatePackageDetailedInfo(docHelper, docFileManager);

		// Table documentation
		generateTablesIndex(docHelper, docFileManager);
		generateTablesSummary(docHelper, docFileManager,
				graphsGenerated, outputDirectory);
		generateTableDetails(docHelper, docFileManager);
		generateAllSchemasList(docHelper, docFileManager);
		generateAllTablesList(docHelper, docFileManager);
		generatePerSchemaTableList(docHelper, docFileManager);
		generateSchemaDetailedInfo(docHelper, docFileManager);
	}

	private void copyAssets(EntityDocFileManager docFileManager) {
		ClassLoader loader = getClass().getClassLoader();
		copyResource(loader, RES_CSS,
				docFileManager.getCssStylesDocFile().getFile());
		copyResource(loader, RES_HIBERNATE_IMAGE,
				docFileManager.getHibernateImageDocFile().getFile());
		copyResource(loader, RES_EXTENDS_IMAGE,
				docFileManager.getExtendsImageDocFile().getFile());
	}

	private void copyMainIndex(EntityDocFileManager docFileManager) {
		ClassLoader loader = getClass().getClassLoader();
		copyResource(loader, RES_MAIN_INDEX,
				docFileManager.getMainIndexDocFile().getFile());
	}

	private void generateEntitiesIndex(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getClassIndexDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		processTemplate(params, FTL_ENTITIES_INDEX, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generatePackageSummary(EntityDocHelper docHelper,
										 EntityDocFileManager docFileManager,
										 boolean graphsGenerated,
										 File outputDirectory) {
		DocFile docFile = docFileManager.getClassSummaryFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		List<String> packageList = docHelper.getPackages();
		if (!packageList.isEmpty()) {
			packageList = new ArrayList<>(packageList);
			packageList.remove(EntityDocHelper.DEFAULT_NO_PACKAGE);
		}
		params.put("packageList", packageList);
		params.put("graphsGenerated", graphsGenerated);
		if (graphsGenerated) {
			params.put("entitygrapharea",
					readFileContent(new File(outputDirectory,
							"entities/entitygraph.cmapx")));
		}
		processTemplate(params, FTL_ENTITIES_SUMMARY, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generateEntitiesDetails(EntityDocHelper docHelper,
										  EntityDocFileManager docFileManager) {
		for (EntityDocInfo entity : docHelper.getClasses()) {
			DocFile docFile = docFileManager.getEntityDocFile(entity);
			Map<String, Object> params = new HashMap<>();
			params.put("docFile", docFile);
			params.put("class", entity);
			processTemplate(params, FTL_ENTITIES_ENTITY, docFile.getFile(),
					docHelper, docFileManager);
		}
	}

	private void generateAllPackagesList(EntityDocHelper docHelper,
										  EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getAllPackagesDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		List<String> packageList = docHelper.getPackages();
		if (!packageList.isEmpty()) {
			packageList = new ArrayList<>(packageList);
			packageList.remove(EntityDocHelper.DEFAULT_NO_PACKAGE);
		}
		params.put("packageList", packageList);
		processTemplate(params, FTL_ENTITIES_PACKAGE_LIST, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generateAllEntitiesList(EntityDocHelper docHelper,
										  EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getAllEntitiesDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		params.put("classList", docHelper.getClasses());
		processTemplate(params, FTL_ENTITIES_ENTITY_LIST, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generatePerPackageEntityList(EntityDocHelper docHelper,
											   EntityDocFileManager docFileManager) {
		for (String packageName : docHelper.getPackages()) {
			if (!packageName.equals(EntityDocHelper.DEFAULT_NO_PACKAGE)) {
				DocFile docFile =
						docFileManager.getPackageEntityListDocFile(packageName);
				Map<String, Object> params = new HashMap<>();
				params.put("docFile", docFile);
				params.put("title", packageName);
				params.put("classList",
						docHelper.getClasses(packageName));
				processTemplate(params,
						FTL_ENTITIES_PERPACKAGE_ENTITY_LIST,
						docFile.getFile(), docHelper, docFileManager);
			}
		}
	}

	private void generatePackageDetailedInfo(EntityDocHelper docHelper,
											  EntityDocFileManager docFileManager) {
		List<String> packageList = docHelper.getPackages();
		if (!packageList.isEmpty()) {
			packageList = new ArrayList<>(packageList);
			packageList.remove(EntityDocHelper.DEFAULT_NO_PACKAGE);
		}
		for (String packageName : packageList) {
			DocFile docFile =
					docFileManager.getPackageSummaryDocFile(packageName);
			Map<String, Object> params = new HashMap<>();
			params.put("docFile", docFile);
			params.put("package", packageName);
			params.put("classList", docHelper.getClasses(packageName));
			processTemplate(params, FTL_ENTITIES_PACKAGE_SUMMARY,
					docFile.getFile(), docHelper, docFileManager);
		}
	}

	// ---- Table generation methods ----

	private void generateTablesIndex(EntityDocHelper docHelper,
									  EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getTableIndexDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		processTemplate(params, FTL_TABLES_INDEX, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generateTablesSummary(EntityDocHelper docHelper,
										 EntityDocFileManager docFileManager,
										 boolean graphsGenerated,
										 File outputDirectory) {
		DocFile docFile = docFileManager.getTableSummaryDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		params.put("graphsGenerated", graphsGenerated);
		if (graphsGenerated) {
			params.put("tablegrapharea",
					readFileContent(new File(outputDirectory,
							"tables/tablegraph.cmapx")));
		}
		processTemplate(params, FTL_TABLES_SUMMARY, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generateTableDetails(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		for (TableDocInfo table : docHelper.getTables()) {
			DocFile docFile = docFileManager.getTableDocFile(table);
			Map<String, Object> params = new HashMap<>();
			params.put("docFile", docFile);
			params.put("table", table);
			processTemplate(params, FTL_TABLES_TABLE, docFile.getFile(),
					docHelper, docFileManager);
		}
	}

	private void generateAllSchemasList(EntityDocHelper docHelper,
										  EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getAllSchemasDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		params.put("schemaList", docHelper.getSchemas());
		processTemplate(params, FTL_TABLES_SCHEMA_LIST, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generateAllTablesList(EntityDocHelper docHelper,
										 EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getAllTablesDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		params.put("tableList", docHelper.getTables());
		processTemplate(params, FTL_TABLES_TABLE_LIST, docFile.getFile(),
				docHelper, docFileManager);
	}

	private void generatePerSchemaTableList(EntityDocHelper docHelper,
											  EntityDocFileManager docFileManager) {
		for (String schema : docHelper.getSchemas()) {
			DocFile docFile =
					docFileManager.getSchemaTableListDocFile(schema);
			Map<String, Object> params = new HashMap<>();
			params.put("docFile", docFile);
			params.put("title", schema);
			params.put("tableList", docHelper.getTables(schema));
			processTemplate(params, FTL_TABLES_SCHEMA_TABLE_LIST,
					docFile.getFile(), docHelper, docFileManager);
		}
	}

	private void generateSchemaDetailedInfo(EntityDocHelper docHelper,
											  EntityDocFileManager docFileManager) {
		for (String schema : docHelper.getSchemas()) {
			DocFile docFile =
					docFileManager.getSchemaSummaryDocFile(schema);
			Map<String, Object> params = new HashMap<>();
			params.put("docFile", docFile);
			params.put("schema", schema);
			processTemplate(params, FTL_TABLES_SCHEMA_SUMMARY,
					docFile.getFile(), docHelper, docFileManager);
		}
	}

	// ---- DOT graph generation ----

	private boolean generateDot(File outputDirectory,
								 EntityDocHelper docHelper,
								 EntityDocFileManager docFileManager) {
		if (dotExecutable == null || dotExecutable.isEmpty()) {
			log.info("Skipping graph generation since dot executable"
					+ " is not specified.");
			return false;
		}

		try {
			// Generate entity graph DOT file
			File entityDotFile = new File(outputDirectory,
					"entities/entitygraph.dot");
			Map<String, Object> entityParams = new HashMap<>();
			entityParams.put("entities", docHelper.getClasses());
			processTemplate(entityParams, FTL_DOT_ENTITY_GRAPH,
					entityDotFile, docHelper, docFileManager);

			// Generate table graph DOT file
			File tableDotFile = new File(outputDirectory,
					"tables/tablegraph.dot");
			Map<String, Object> tableParams = new HashMap<>();
			tableParams.put("tables", docHelper.getTables());
			processTemplate(tableParams, FTL_DOT_TABLE_GRAPH,
					tableDotFile, docHelper, docFileManager);

			// Convert to PNG, SVG, and CMAPX
			dotToFile(entityDotFile.toString(),
					new File(outputDirectory,
							"entities/entitygraph.png").toString());
			dotToFile(entityDotFile.toString(),
					new File(outputDirectory,
							"entities/entitygraph.svg").toString());
			dotToFile(entityDotFile.toString(),
					new File(outputDirectory,
							"entities/entitygraph.cmapx").toString());

			dotToFile(tableDotFile.toString(),
					new File(outputDirectory,
							"tables/tablegraph.png").toString());
			dotToFile(tableDotFile.toString(),
					new File(outputDirectory,
							"tables/tablegraph.svg").toString());
			dotToFile(tableDotFile.toString(),
					new File(outputDirectory,
							"tables/tablegraph.cmapx").toString());

			return true;
		}
		catch (IOException e) {
			log.log(Level.WARNING,
					"Skipping graph generation due to error", e);
			return false;
		}
	}

	private void dotToFile(String dotFileName, String outFileName)
			throws IOException {
		String format = outFileName.substring(
				outFileName.lastIndexOf('.') + 1);
		String exe = escapeFileName(dotExecutable);
		String[] cmd = {
				exe, "-T", format,
				escapeFileName(dotFileName),
				"-o", escapeFileName(outFileName)
		};

		Process p = Runtime.getRuntime().exec(cmd);
		try {
			InputStream errStream = p.getErrorStream();
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = errStream.read()) != -1) {
				sb.append((char) c);
			}
			errStream.close();
			if (!sb.isEmpty()) {
				log.warning(sb.toString());
			}
			int exitCode = p.waitFor();
			if (exitCode != 0) {
				log.warning("dot exited with code " + exitCode
						+ " for " + outFileName);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while running dot", e);
		}
	}

	private static String escapeFileName(String fileName) {
		if (System.getProperty("os.name", "").startsWith("Linux")) {
			return fileName;
		}
		return "\"" + fileName + "\"";
	}

	private static String readFileContent(File file) {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line).append(System.lineSeparator());
			}
		}
		catch (IOException ignored) {
		}
		return sb.toString();
	}

	private void processTemplate(Map<String, Object> params,
								  String templateName, File outputFile,
								  EntityDocHelper docHelper,
								  EntityDocFileManager docFileManager) {
		// Use SimpleHash to avoid BeansWrapper/MapModel intercepting
		// reserved keys like "class" (which would call getClass())
		SimpleHash model = new SimpleHash(beansWrapper);
		model.put("dochelper", docHelper);
		model.put("docFileManager", docFileManager);
		model.put("jdk5", true);
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			model.put(entry.getKey(), entry.getValue());
		}
		try (Writer writer = new FileWriter(outputFile)) {
			Template template = freemarkerConfig.getTemplate(templateName);
			template.process(model, writer);
		}
		catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Failed to process template " + templateName
					+ " to " + outputFile, e);
		}
	}

	private static void copyResource(ClassLoader loader, String resourceName,
									   File target) {
		try (InputStream is = loader.getResourceAsStream(resourceName)) {
			if (is == null) {
				throw new IllegalArgumentException(
						"Resource not found: " + resourceName);
			}
			try (FileOutputStream out = new FileOutputStream(target)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Failed to copy resource " + resourceName
					+ " to " + target, e);
		}
	}

	private TemplateLoader createTemplateLoader(String[] templatePath) {
		List<TemplateLoader> loaders = new ArrayList<>();
		if (templatePath != null) {
			for (String path : templatePath) {
				File dir = new File(path);
				if (dir.isDirectory()) {
					try {
						loaders.add(new FileTemplateLoader(dir));
					}
					catch (IOException e) {
						throw new RuntimeException(
								"Failed to create template loader for: "
								+ path, e);
					}
				}
			}
		}
		// Load templates from classpath root so /doc/common.ftl resolves
		loaders.add(new ClassTemplateLoader(
				getClass().getClassLoader(), "/"));
		return new MultiTemplateLoader(
				loaders.toArray(new TemplateLoader[0]));
	}
}
