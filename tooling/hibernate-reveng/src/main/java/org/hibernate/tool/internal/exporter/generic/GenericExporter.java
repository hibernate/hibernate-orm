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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.exporter.MetadataHelper;
import org.hibernate.tool.internal.exporter.entity.ImportContextImpl;
import org.hibernate.tool.internal.exporter.entity.TemplateHelper;
import org.hibernate.tool.api.version.Version;

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

	private static final String HIBERNATETOOL_PREFIX = "hibernatetool.";

	private List<ClassDetails> entities;
	private MetadataHelper metadataHelper;
	private String templateName;
	private String filePattern;
	private String forEach;
	private Configuration freemarkerConfig;
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
		String tmplName = (String) exporterProperties.get(ExporterConstants.TEMPLATE_NAME);
		String filePat = (String) exporterProperties.get(ExporterConstants.FILE_PATTERN);
		String fe = (String) exporterProperties.get(ExporterConstants.FOR_EACH);
		GenericExporter configured = create(md, tmplName, filePat, fe, templatePath);
		configured.exporterProperties = this.exporterProperties;
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
		MetadataHelper helper = MetadataHelper.from(md);
		GenericExporter exporter = new GenericExporter(
				helper.getEntityClassDetails(),
				templateName, filePattern, null, new String[0]);
		exporter.metadataHelper = helper;
		return exporter;
	}

	public static GenericExporter create(MetadataDescriptor md,
										  String templateName,
										  String filePattern,
										  String forEach,
										  String[] templatePath) {
		MetadataHelper helper = MetadataHelper.from(md);
		GenericExporter exporter = new GenericExporter(
				helper.getEntityClassDetails(),
				templateName, filePattern, forEach, templatePath);
		exporter.metadataHelper = helper;
		return exporter;
	}

	public void exportAll(File outputDir) {
		this.outputDir = outputDir;
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
							.toList(), "Entity");
					break;
				case "component":
					exportPerClass(outputDir, entities.stream()
							.filter(this::isEmbeddable)
							.toList(), "Component");
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
		Map<String, Object> model = buildModel();
		String outputClassName = resolveOutputClassName(entity);
		String simpleName = outputClassName.contains(".")
				? outputClassName.substring(outputClassName.lastIndexOf('.') + 1)
				: outputClassName;
		String packageName = outputClassName.contains(".")
				? outputClassName.substring(0, outputClassName.lastIndexOf('.'))
				: getPackageName(entity);
		ModelsContext mc = metadataHelper != null ? metadataHelper.getModelsContext() : null;
		Map<String, List<String>> classMeta = metadataHelper != null
				? metadataHelper.getClassMetaAttributes(entity.getClassName())
				: Collections.emptyMap();
		Map<String, Map<String, List<String>>> fieldMeta = metadataHelper != null
				? metadataHelper.getFieldMetaAttributes(entity.getClassName())
				: Collections.emptyMap();
		ImportContextImpl importContext = new ImportContextImpl(packageName);
		TemplateHelper templateHelper = new TemplateHelper(
				entity, mc, importContext, false,
				classMeta, fieldMeta);
		model.put("templateHelper", templateHelper);
		model.put("clazz", entity);
		model.put("entity", entity);
		model.put("className", simpleName);
		model.put("packageName", packageName);
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

	// ---- Template model support ----

	/**
	 * Template helper that allows FreeMarker templates to create output files.
	 */
	public class Templates {
		public void createFile(String content, String fileName) {
			File target = new File(outputDir, fileName);
			target.getParentFile().mkdirs();
			try (Writer fw = new BufferedWriter(new FileWriter(target))) {
				fw.write(content);
			} catch (IOException e) {
				throw new RuntimeException("Problem when writing to " + fileName, e);
			}
		}
	}

	// ---- Private implementation ----

	private Map<String, Object> buildModel() {
		Map<String, Object> model = new HashMap<>();
		model.put("date", new Date());
		model.put("version", Version.versionString());
		model.put("templates", new Templates());
		// Add ArtifactCollector as "artifacts" (matches old AbstractExporter context)
		if (exporterProperties != null) {
			Object ac = exporterProperties.get(ExporterConstants.ARTIFACT_COLLECTOR);
			model.put("artifacts", ac != null ? ac
					: new org.hibernate.tool.api.export.DefaultArtifactCollector());
		}
		// Add exporter properties to the model (with hibernatetool. prefix handling)
		if (exporterProperties != null) {
			for (Map.Entry<Object, Object> entry : exporterProperties.entrySet()) {
				String key = entry.getKey().toString();
				Object value = transformValue(entry.getValue());
				model.put(key, value);
				if (key.startsWith(HIBERNATETOOL_PREFIX)) {
					String shortKey = key.substring(HIBERNATETOOL_PREFIX.length());
					model.put(shortKey, value);
					if (key.endsWith(".toolclass")) {
						String toolKey = shortKey.substring(
								0, shortKey.length() - ".toolclass".length());
						try {
							Class<?> toolClass = Class.forName(value.toString());
							Constructor<?> ctor = toolClass.getConstructor();
							model.put(toolKey, ctor.newInstance());
						} catch (Exception e) {
							throw new RuntimeException(
									"Exception when instantiating tool "
									+ key + " with " + value, e);
						}
					}
				}
			}
		}
		// Add ctx as the model itself, for templates that iterate over context keys
		model.put("ctx", model);
		return model;
	}

	private Object transformValue(Object value) {
		if ("true".equals(value)) return Boolean.TRUE;
		if ("false".equals(value)) return Boolean.FALSE;
		return value;
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

	private void exportPerClass(File outputDir, List<ClassDetails> classes, String entityType) {
		for (ClassDetails entity : classes) {
			String filename = resolveFilename(entity);
			File outputFile = new File(outputDir, filename);
			outputFile.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(outputFile)) {
				export(writer, entity);
			} catch (RuntimeException e) {
				throw new RuntimeException(
						"Error while processing " + entityType + ": "
						+ getSimpleName(entity), e);
			} catch (IOException e) {
				throw new RuntimeException(
						"Error while processing " + entityType + ": "
						+ getSimpleName(entity), e);
			}
		}
	}

	private void exportConfiguration(File outputDir) {
		File outputFile = new File(outputDir, filePattern);
		outputFile.getParentFile().mkdirs();
		Map<String, Object> model = buildModel();
		model.put("entities", entities);
		try (Writer writer = new FileWriter(outputFile)) {
			processTemplate(model, writer);
		} catch (RuntimeException e) {
			throw new RuntimeException(
					"Error while processing Configuration", e);
		} catch (IOException e) {
			throw new RuntimeException(
					"Error while processing Configuration", e);
		}
	}

	private String resolveFilename(ClassDetails entity) {
		String outputClassName = resolveOutputClassName(entity);
		String simpleName = outputClassName.contains(".")
				? outputClassName.substring(outputClassName.lastIndexOf('.') + 1)
				: outputClassName;
		String filename = filePattern.replace("{class-name}", simpleName);
		String packageName = outputClassName.contains(".")
				? outputClassName.substring(0, outputClassName.lastIndexOf('.'))
				: getPackageName(entity);
		String packagePath = packageName.replace('.', '/');
		if (packagePath.isEmpty()) {
			packagePath = ".";
		}
		filename = filename.replace("{package-name}", packagePath);
		return filename;
	}

	private String resolveOutputClassName(ClassDetails entity) {
		if (metadataHelper != null) {
			Map<String, List<String>> classMeta =
					metadataHelper.getClassMetaAttributes(entity.getClassName());
			List<String> generatedClass = classMeta.getOrDefault(
					"generated-class", Collections.emptyList());
			if (!generatedClass.isEmpty()) {
				return generatedClass.get(0).trim();
			}
		}
		return entity.getClassName();
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
