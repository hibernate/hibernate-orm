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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.version.Version;

import java.util.Properties;

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
	private String templateName;
	private String filePattern;
	private String forEach;
	private Configuration freemarkerConfig;
	private Properties exporterProperties = new Properties();

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
		String tmplName = (String) exporterProperties.get(ExporterConstants.TEMPLATE_NAME);
		String filePat = (String) exporterProperties.get(ExporterConstants.FILE_PATTERN);
		String fe = (String) exporterProperties.get(ExporterConstants.FOR_EACH);
		GenericExporter configured = create(md, tmplName, filePat, fe, templatePath);
		configured.exportAll(destDir);
	}

	private GenericExporter(List<ClassDetails> entities, String templateName,
							String filePattern, String forEach,
							String[] templatePath) {
		this.entities = entities;
		this.templateName = templateName;
		this.filePattern = filePattern;
		this.forEach = forEach;
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
	}

	public static GenericExporter create(List<ClassDetails> entities,
										  String templateName,
										  String filePattern) {
		return new GenericExporter(entities, templateName, filePattern, null, new String[0]);
	}

	public static GenericExporter create(List<ClassDetails> entities,
										  String templateName,
										  String filePattern,
										  String forEach) {
		return new GenericExporter(entities, templateName, filePattern, forEach, new String[0]);
	}

	public static GenericExporter create(List<ClassDetails> entities,
										  String templateName,
										  String filePattern,
										  String forEach,
										  String[] templatePath) {
		return new GenericExporter(entities, templateName, filePattern, forEach, templatePath);
	}

	public static GenericExporter create(MetadataDescriptor md,
										  String templateName,
										  String filePattern) {
		return new GenericExporter(md.getEntityClassDetails(), templateName, filePattern, null, new String[0]);
	}

	public static GenericExporter create(MetadataDescriptor md,
										  String templateName,
										  String filePattern,
										  String forEach,
										  String[] templatePath) {
		return new GenericExporter(md.getEntityClassDetails(), templateName, filePattern,
				forEach, templatePath);
	}

	public void exportAll(File outputDir) {
		if (templateName == null) {
			throw new RuntimeException("Template name not set on " + getClass());
		}
		if (filePattern == null) {
			throw new RuntimeException("File pattern not set on " + getClass());
		}

		List<String> modes = resolveModes();
		for (String mode : modes) {
			switch (mode) {
				case "entity":
					exportPerClass(outputDir, entities.stream()
							.filter(e -> !isEmbeddable(e))
							.toList());
					break;
				case "component":
					exportPerClass(outputDir, entities.stream()
							.filter(this::isEmbeddable)
							.toList());
					break;
				case "configuration":
					exportConfiguration(outputDir);
					break;
				default:
					throw new RuntimeException(
							"for-each does not support [" + mode + "]");
			}
		}
	}

	public void export(Writer output, ClassDetails entity) {
		Map<String, Object> model = new HashMap<>();
		model.put("entity", entity);
		model.put("className", getSimpleName(entity));
		model.put("packageName", getPackageName(entity));
		model.put("date", new Date());
		model.put("version", Version.versionString());
		processTemplate(model, output);
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

	private List<String> resolveModes() {
		List<String> modes = new ArrayList<>();
		if (forEach == null || forEach.isEmpty()) {
			if (filePattern.contains("{class-name}")) {
				modes.add("entity");
				modes.add("component");
			} else {
				modes.add("configuration");
			}
		} else {
			StringTokenizer tokens = new StringTokenizer(forEach, ",");
			while (tokens.hasMoreTokens()) {
				modes.add(tokens.nextToken().trim());
			}
		}
		return modes;
	}

	private void exportPerClass(File outputDir, List<ClassDetails> classes) {
		for (ClassDetails entity : classes) {
			String filename = resolveFilename(entity);
			File outputFile = new File(outputDir, filename);
			outputFile.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(outputFile)) {
				export(writer, entity);
			} catch (IOException e) {
				throw new RuntimeException(
						"Failed to export " + entity.getClassName()
						+ " to " + outputFile, e);
			}
		}
	}

	private void exportConfiguration(File outputDir) {
		File outputFile = new File(outputDir, filePattern);
		outputFile.getParentFile().mkdirs();
		Map<String, Object> model = new HashMap<>();
		model.put("entities", entities);
		model.put("date", new Date());
		model.put("version", Version.versionString());
		try (Writer writer = new FileWriter(outputFile)) {
			processTemplate(model, writer);
		} catch (IOException e) {
			throw new RuntimeException(
					"Failed to export configuration to " + outputFile, e);
		}
	}

	private String resolveFilename(ClassDetails entity) {
		String filename = filePattern.replace(
				"{class-name}", getSimpleName(entity));
		String packagePath = getPackageName(entity).replace('.', '/');
		if (packagePath.isEmpty()) {
			packagePath = ".";
		}
		filename = filename.replace("{package-name}", packagePath);
		return filename;
	}

	private boolean isEmbeddable(ClassDetails entity) {
		return entity.hasDirectAnnotationUsage(jakarta.persistence.Embeddable.class);
	}

	private String getSimpleName(ClassDetails entity) {
		String className = entity.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(lastDot + 1) : className;
	}

	private String getPackageName(ClassDetails entity) {
		String className = entity.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	private void processTemplate(Map<String, Object> model, Writer output) {
		try {
			Template template = freemarkerConfig.getTemplate(templateName);
			template.process(model, output);
			output.flush();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Error processing template " + templateName, e);
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
					} catch (IOException e) {
						throw new RuntimeException(
								"Failed to create template loader for: " + path, e);
					}
				}
			}
		}
		loaders.add(new ClassTemplateLoader(getClass().getClassLoader(), "/"));
		return new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0]));
	}
}
