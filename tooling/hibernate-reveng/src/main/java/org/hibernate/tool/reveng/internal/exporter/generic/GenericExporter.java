/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.generic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.util.MetadataHelper;
import org.hibernate.tool.reveng.internal.exporter.entity.ImportContext;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper;

/**
 * A generic template-based exporter that applies a user-specified FreeMarker
 * template to each entity (or once for the whole configuration).
 * <p>
 * Supports three iteration modes via {@code forEach}:
 * <ul>
 *   <li><b>entity</b> — render the template once per entity</li>
 *   <li><b>component</b> — render the template once per embeddable</li>
 *   <li><b>configuration</b> — render the template once (no per-entity iteration)</li>
 * </ul>
 * If {@code forEach} is not set, the mode is inferred from the file pattern:
 * if it contains {@code {class-name}}, both entity and component modes run;
 * otherwise configuration mode runs.
 *
 * @author Koen Aers
 */
public class GenericExporter implements Exporter {

	private List<ClassDetails> entities;
	private MetadataHelper metadataHelper;
	private String templateName;
	private String filePattern;
	private String forEach;
	private GenericTemplateProcessor processor;
	private Properties exporterProperties = new Properties();
	private File outputDir;

	public GenericExporter() {}

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
		String tmplName = (String)
				exporterProperties.get(ExporterConstants.TEMPLATE_NAME);
		String filePat = (String)
				exporterProperties.get(ExporterConstants.FILE_PATTERN);
		String fe = (String)
				exporterProperties.get(ExporterConstants.FOR_EACH);
		GenericExporter configured =
				create(md, tmplName, filePat, fe, templatePath);
		configured.exporterProperties = this.exporterProperties;
		configured.exportAll(destDir);
	}

	private GenericExporter(
			List<ClassDetails> entities, String templateName,
			String filePattern, String forEach,
			String[] templatePath) {
		this.entities = entities;
		this.templateName = templateName;
		this.filePattern = filePattern;
		this.forEach = forEach;
		this.processor = templateName != null
				? new GenericTemplateProcessor(templateName, templatePath)
				: null;
	}

	public static GenericExporter create(
			List<ClassDetails> entities, String templateName,
			String filePattern) {
		return new GenericExporter(
				entities, templateName, filePattern,
				null, new String[0]);
	}

	public static GenericExporter create(
			List<ClassDetails> entities, String templateName,
			String filePattern, String forEach) {
		return new GenericExporter(
				entities, templateName, filePattern,
				forEach, new String[0]);
	}

	public static GenericExporter create(
			List<ClassDetails> entities, String templateName,
			String filePattern, String forEach,
			String[] templatePath) {
		return new GenericExporter(
				entities, templateName, filePattern,
				forEach, templatePath);
	}

	public static GenericExporter create(
			MetadataDescriptor md, String templateName,
			String filePattern) {
		MetadataHelper helper = MetadataHelper.from(md);
		GenericExporter exporter = new GenericExporter(
				helper.getEntityClassDetails(), templateName,
				filePattern, null, new String[0]);
		exporter.metadataHelper = helper;
		return exporter;
	}

	public static GenericExporter create(
			MetadataDescriptor md, String templateName,
			String filePattern, String forEach,
			String[] templatePath) {
		MetadataHelper helper = MetadataHelper.from(md);
		GenericExporter exporter = new GenericExporter(
				helper.getEntityClassDetails(), templateName,
				filePattern, forEach, templatePath);
		exporter.metadataHelper = helper;
		return exporter;
	}

	public void exportAll(File outputDir) {
		this.outputDir = outputDir;
		if (templateName == null) {
			throw new RuntimeException(
					"Template name not set on " + getClass());
		}
		if (filePattern == null) {
			throw new RuntimeException(
					"File pattern not set on " + getClass());
		}
		List<String> modes = GenericTemplateProcessor.resolveModes(
				forEach, filePattern);
		for (String mode : modes) {
			switch (mode) {
				case "entity":
					processor.exportPerClass(
							outputDir, filePattern,
							entities.stream()
									.filter(e -> !isEmbeddable(e))
									.toList(),
							"Entity", metadataHelper, this::export);
					break;
				case "component":
					processor.exportPerClass(
							outputDir, filePattern,
							entities.stream()
									.filter(this::isEmbeddable)
									.toList(),
							"Component", metadataHelper,
							this::export);
					break;
				case "configuration":
					processor.exportConfiguration(
							outputDir, filePattern,
							exporterProperties, entities,
							new Templates());
					break;
				default:
					throw new RuntimeException(
							"for-each does not support ["
							+ mode + "]");
			}
		}
	}

	public void export(Writer output, ClassDetails entity) {
		Map<String, Object> model = processor.buildModel(
				exporterProperties, new Templates());
		String outputClassName =
				GenericTemplateProcessor.resolveOutputClassName(
						entity, metadataHelper);
		String simpleName = outputClassName.contains(".")
				? outputClassName.substring(
						outputClassName.lastIndexOf('.') + 1)
				: outputClassName;
		String packageName = outputClassName.contains(".")
				? outputClassName.substring(
						0, outputClassName.lastIndexOf('.'))
				: getPackageName(entity);
		ModelsContext mc = metadataHelper != null
				? metadataHelper.getModelsContext() : null;
		Map<String, List<String>> classMeta =
				metadataHelper != null
				? metadataHelper.getClassMetaAttributes(
						entity.getClassName())
				: Collections.emptyMap();
		Map<String, Map<String, List<String>>> fieldMeta =
				metadataHelper != null
				? metadataHelper.getFieldMetaAttributes(
						entity.getClassName())
				: Collections.emptyMap();
		ImportContext importContext =
				new ImportContext(packageName);
		TemplateHelper templateHelper = new TemplateHelper(
				entity, mc, importContext, false,
				classMeta, fieldMeta);
		model.put("templateHelper", templateHelper);
		model.put("classInfo", templateHelper.getClassAnnotationGenerator());
		model.put("fieldAnnotations", templateHelper.getFieldAnnotationGenerator());
		model.put("relAnnotations", templateHelper.getRelationshipAnnotationGenerator());
		model.put("constructors", templateHelper.getConstructorHelper());
		model.put("equalsHashCode", templateHelper.getEqualsHashCodeHelper());
		model.put("meta", templateHelper.getMetaAttributeSupport());
		model.put("queries", templateHelper.getQueryAndFilterHelper());
		model.put("clazz", entity);
		model.put("entity", entity);
		model.put("className", simpleName);
		model.put("packageName", packageName);
		processor.processTemplate(model, output);
	}

	public List<ClassDetails> getEntities() {
		return entities;
	}

	public String getTemplateName() {
		return templateName;
	}

	public String getFilePattern() {
		return filePattern;
	}

	public String getForEach() {
		return forEach;
	}

	/**
	 * Template helper that allows FreeMarker templates to
	 * create output files.
	 */
	public class Templates {
		public void createFile(String content, String fileName) {
			File target = new File(outputDir, fileName);
			target.getParentFile().mkdirs();
			try (Writer fw =
					new BufferedWriter(new FileWriter(target))) {
				fw.write(content);
			}
		catch (IOException e) {
				throw new RuntimeException(
						"Problem when writing to " + fileName, e);
			}
		}
	}

	private boolean isEmbeddable(ClassDetails entity) {
		return entity.hasDirectAnnotationUsage(
				jakarta.persistence.Embeddable.class);
	}

	private String getPackageName(ClassDetails entity) {
		String className = entity.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0
				? className.substring(0, lastDot) : "";
	}
}
