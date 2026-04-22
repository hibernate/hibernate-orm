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
import java.util.function.BiConsumer;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.version.Version;
import org.hibernate.tool.internal.util.MetadataHelper;

class GenericTemplateProcessor {

	private static final String HIBERNATETOOL_PREFIX = "hibernatetool.";

	private final Configuration freemarkerConfig;
	private final String templateName;

	GenericTemplateProcessor(String templateName, String[] templatePath) {
		this.templateName = templateName;
		this.freemarkerConfig =
				new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(
				createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
	}

	void processTemplate(Map<String, Object> model, Writer output) {
		try {
			Template template =
					freemarkerConfig.getTemplate(templateName);
			template.process(model, output);
			output.flush();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Error processing template " + templateName, e);
		}
	}

	Map<String, Object> buildModel(
			Properties exporterProperties, Object templates) {
		Map<String, Object> model = new HashMap<>();
		model.put("date", new Date());
		model.put("version", Version.versionString());
		model.put("templates", templates);
		if (exporterProperties != null) {
			Object ac = exporterProperties.get(
					ExporterConstants.ARTIFACT_COLLECTOR);
			model.put("artifacts", ac != null ? ac
					: new org.hibernate.tool.api.export
							.DefaultArtifactCollector());
		}
		addExporterProperties(model, exporterProperties);
		model.put("ctx", model);
		return model;
	}

	void exportPerClass(
			File outputDir, String filePattern,
			List<ClassDetails> classes, String entityType,
			MetadataHelper metadataHelper,
			BiConsumer<Writer, ClassDetails> entityExporter) {
		for (ClassDetails entity : classes) {
			String filename = resolveFilename(
					entity, filePattern, metadataHelper);
			File outputFile = new File(outputDir, filename);
			outputFile.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(outputFile)) {
				entityExporter.accept(writer, entity);
			} catch (RuntimeException e) {
				throw new RuntimeException(
						"Error while processing " + entityType
						+ ": " + getSimpleName(entity), e);
			} catch (IOException e) {
				throw new RuntimeException(
						"Error while processing " + entityType
						+ ": " + getSimpleName(entity), e);
			}
		}
	}

	void exportConfiguration(
			File outputDir, String filePattern,
			Properties exporterProperties,
			List<ClassDetails> entities, Object templates) {
		File outputFile = new File(outputDir, filePattern);
		outputFile.getParentFile().mkdirs();
		Map<String, Object> model =
				buildModel(exporterProperties, templates);
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

	static List<String> resolveModes(
			String forEach, String filePattern) {
		List<String> modes = new ArrayList<>();
		if (forEach == null || forEach.isEmpty()) {
			if (filePattern.contains("{class-name}")) {
				modes.add("entity");
				modes.add("component");
			} else {
				modes.add("configuration");
			}
		} else {
			StringTokenizer tokens =
					new StringTokenizer(forEach, ",");
			while (tokens.hasMoreTokens()) {
				modes.add(tokens.nextToken().trim());
			}
		}
		return modes;
	}

	static String resolveFilename(
			ClassDetails entity, String filePattern,
			MetadataHelper metadataHelper) {
		String outputClassName =
				resolveOutputClassName(entity, metadataHelper);
		String simpleName = outputClassName.contains(".")
				? outputClassName.substring(
						outputClassName.lastIndexOf('.') + 1)
				: outputClassName;
		String filename = filePattern.replace(
				"{class-name}", simpleName);
		String packageName = outputClassName.contains(".")
				? outputClassName.substring(
						0, outputClassName.lastIndexOf('.'))
				: getPackageName(entity);
		String packagePath = packageName.replace('.', '/');
		if (packagePath.isEmpty()) {
			packagePath = ".";
		}
		return filename.replace("{package-name}", packagePath);
	}

	static String resolveOutputClassName(
			ClassDetails entity, MetadataHelper metadataHelper) {
		if (metadataHelper != null) {
			Map<String, List<String>> classMeta =
					metadataHelper.getClassMetaAttributes(
							entity.getClassName());
			List<String> generatedClass = classMeta.getOrDefault(
					"generated-class", Collections.emptyList());
			if (!generatedClass.isEmpty()) {
				return generatedClass.get(0).trim();
			}
		}
		return entity.getClassName();
	}

	private void addExporterProperties(
			Map<String, Object> model, Properties props) {
		if (props == null) return;
		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			String key = entry.getKey().toString();
			Object value = transformValue(entry.getValue());
			model.put(key, value);
			if (key.startsWith(HIBERNATETOOL_PREFIX)) {
				String shortKey = key.substring(
						HIBERNATETOOL_PREFIX.length());
				model.put(shortKey, value);
				if (key.endsWith(".toolclass")) {
					instantiateTool(model, key, shortKey, value);
				}
			}
		}
	}

	private void instantiateTool(
			Map<String, Object> model, String key,
			String shortKey, Object value) {
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

	private static Object transformValue(Object value) {
		if ("true".equals(value)) return Boolean.TRUE;
		if ("false".equals(value)) return Boolean.FALSE;
		return value;
	}

	private static String getSimpleName(ClassDetails entity) {
		String className = entity.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0
				? className.substring(lastDot + 1) : className;
	}

	private static String getPackageName(ClassDetails entity) {
		String className = entity.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0
				? className.substring(0, lastDot) : "";
	}

	private TemplateLoader createTemplateLoader(
			String[] templatePath) {
		List<TemplateLoader> loaders = new ArrayList<>();
		if (templatePath != null) {
			for (String path : templatePath) {
				File dir = new File(path);
				if (dir.isDirectory()) {
					try {
						loaders.add(new FileTemplateLoader(dir));
					} catch (IOException e) {
						throw new RuntimeException(
								"Failed to create template loader"
								+ " for: " + path, e);
					}
				}
			}
		}
		loaders.add(new ClassTemplateLoader(
				getClass().getClassLoader(), "/"));
		return new MultiTemplateLoader(
				loaders.toArray(new TemplateLoader[0]));
	}
}
