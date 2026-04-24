/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.doc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.util.MetadataHelper;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;

/**
 * Generates HTML documentation for entities from a list of
 * {@link ClassDetails}. Uses the existing FreeMarker templates in
 * {@code doc/entities/*.ftl} with adapter objects that present
 * {@code ClassDetails}/{@code FieldDetails} in a shape compatible
 * with the template expectations.
 * <p>
 * This is the entity-side replacement for the old {@code DocExporter}.
 * Table-side documentation can be added separately.
 *
 * @author Koen Aers
 */
public class DocExporter implements Exporter {

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

	private List<ClassDetails> entities;
	private Map<String, TableDescriptor> tableMetadataMap;
	private String dotExecutable;
	private DocTemplateRenderer renderer;

	private Properties exporterProperties = new Properties();

	public DocExporter() {}

	@Override
	public Properties getProperties() {
		return exporterProperties;
	}

	@Override
	public void start() {
		MetadataDescriptor md = (MetadataDescriptor)
				exporterProperties.get(ExporterConstants.METADATA_DESCRIPTOR);
		File destDir = (File)
				exporterProperties.get(ExporterConstants.DESTINATION_FOLDER);
		String[] templatePath = (String[])
				exporterProperties.get(ExporterConstants.TEMPLATE_PATH);
		if (templatePath == null) templatePath = new String[0];
		String dotExec = exporterProperties.getProperty("dot.executable");
		DocExporter configured = create(
				MetadataHelper.from(md).getEntityClassDetails(),
				null, dotExec, templatePath);
		configured.export(destDir);
	}

	private DocExporter(List<ClassDetails> entities,
						Map<String, TableDescriptor> tableMetadataMap,
						String dotExecutable,
						String[] templatePath) {
		this.entities = entities;
		this.tableMetadataMap = tableMetadataMap;
		this.dotExecutable = dotExecutable;
		this.renderer = new DocTemplateRenderer(templatePath);
	}

	public static DocExporter create(List<ClassDetails> entities) {
		return new DocExporter(entities, null, null, new String[0]);
	}

	public static DocExporter create(List<ClassDetails> entities,
									String[] templatePath) {
		return new DocExporter(entities, null, null, templatePath);
	}

	public static DocExporter create(List<ClassDetails> entities,
									Map<String, TableDescriptor> tableMetadataMap) {
		return new DocExporter(entities, tableMetadataMap, null,
				new String[0]);
	}

	public static DocExporter create(List<ClassDetails> entities,
									Map<String, TableDescriptor> tableMetadataMap,
									String[] templatePath) {
		return new DocExporter(entities, tableMetadataMap, null,
				templatePath);
	}

	public static DocExporter create(List<ClassDetails> entities,
									Map<String, TableDescriptor> tableMetadataMap,
									String dotExecutable,
									String[] templatePath) {
		return new DocExporter(entities, tableMetadataMap, dotExecutable,
				templatePath);
	}

	public static DocExporter create(MetadataDescriptor md) {
		return new DocExporter(
				MetadataHelper.from(md).getEntityClassDetails(),
				null, null, new String[0]);
	}

	public static DocExporter create(MetadataDescriptor md, String[] templatePath) {
		return new DocExporter(
				MetadataHelper.from(md).getEntityClassDetails(),
				null, null, templatePath);
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

		boolean graphsGenerated = renderer.generateDot(
				outputDirectory, dotExecutable, docHelper, docFileManager,
				FTL_DOT_ENTITY_GRAPH, FTL_DOT_TABLE_GRAPH);

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
		DocTemplateRenderer.copyResource(loader, RES_CSS,
				docFileManager.getCssStylesDocFile().getFile());
		DocTemplateRenderer.copyResource(loader, RES_HIBERNATE_IMAGE,
				docFileManager.getHibernateImageDocFile().getFile());
		DocTemplateRenderer.copyResource(loader, RES_EXTENDS_IMAGE,
				docFileManager.getExtendsImageDocFile().getFile());
	}

	private void copyMainIndex(EntityDocFileManager docFileManager) {
		ClassLoader loader = getClass().getClassLoader();
		DocTemplateRenderer.copyResource(loader, RES_MAIN_INDEX,
				docFileManager.getMainIndexDocFile().getFile());
	}

	private void generateEntitiesIndex(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getClassIndexDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		renderer.processTemplate(params, FTL_ENTITIES_INDEX,
				docFile.getFile(), docHelper, docFileManager);
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
					DocTemplateRenderer.readFileContent(new File(outputDirectory,
							"entities/entitygraph.cmapx")));
		}
		renderer.processTemplate(params, FTL_ENTITIES_SUMMARY,
				docFile.getFile(), docHelper, docFileManager);
	}

	private void generateEntitiesDetails(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		for (EntityDocInfo entity : docHelper.getClasses()) {
			DocFile docFile = docFileManager.getEntityDocFile(entity);
			Map<String, Object> params = new HashMap<>();
			params.put("docFile", docFile);
			params.put("class", entity);
			renderer.processTemplate(params, FTL_ENTITIES_ENTITY,
					docFile.getFile(), docHelper, docFileManager);
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
		renderer.processTemplate(params, FTL_ENTITIES_PACKAGE_LIST,
				docFile.getFile(), docHelper, docFileManager);
	}

	private void generateAllEntitiesList(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getAllEntitiesDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		params.put("classList", docHelper.getClasses());
		renderer.processTemplate(params, FTL_ENTITIES_ENTITY_LIST,
				docFile.getFile(), docHelper, docFileManager);
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
				renderer.processTemplate(params,
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
			renderer.processTemplate(params, FTL_ENTITIES_PACKAGE_SUMMARY,
					docFile.getFile(), docHelper, docFileManager);
		}
	}

	// ---- Table generation methods ----

	private void generateTablesIndex(EntityDocHelper docHelper,
									EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getTableIndexDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		renderer.processTemplate(params, FTL_TABLES_INDEX,
				docFile.getFile(), docHelper, docFileManager);
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
					DocTemplateRenderer.readFileContent(new File(outputDirectory,
							"tables/tablegraph.cmapx")));
		}
		renderer.processTemplate(params, FTL_TABLES_SUMMARY,
				docFile.getFile(), docHelper, docFileManager);
	}

	private void generateTableDetails(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		for (TableDocInfo table : docHelper.getTables()) {
			DocFile docFile = docFileManager.getTableDocFile(table);
			Map<String, Object> params = new HashMap<>();
			params.put("docFile", docFile);
			params.put("table", table);
			renderer.processTemplate(params, FTL_TABLES_TABLE,
					docFile.getFile(), docHelper, docFileManager);
		}
	}

	private void generateAllSchemasList(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getAllSchemasDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		params.put("schemaList", docHelper.getSchemas());
		renderer.processTemplate(params, FTL_TABLES_SCHEMA_LIST,
				docFile.getFile(), docHelper, docFileManager);
	}

	private void generateAllTablesList(EntityDocHelper docHelper,
										EntityDocFileManager docFileManager) {
		DocFile docFile = docFileManager.getAllTablesDocFile();
		Map<String, Object> params = new HashMap<>();
		params.put("docFile", docFile);
		params.put("tableList", docHelper.getTables());
		renderer.processTemplate(params, FTL_TABLES_TABLE_LIST,
				docFile.getFile(), docHelper, docFileManager);
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
			renderer.processTemplate(params, FTL_TABLES_SCHEMA_TABLE_LIST,
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
			renderer.processTemplate(params, FTL_TABLES_SCHEMA_SUMMARY,
					docFile.getFile(), docHelper, docFileManager);
		}
	}
}
