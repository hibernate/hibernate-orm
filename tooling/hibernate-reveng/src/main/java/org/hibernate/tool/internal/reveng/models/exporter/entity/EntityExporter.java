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
package org.hibernate.tool.internal.reveng.models.exporter.entity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.hibernate.tool.api.export.ArtifactCollector;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.exporter.MetadataHelper;
import org.hibernate.tool.api.version.Version;
import org.hibernate.tool.internal.reveng.models.exporter.EntityFileWriter;

/**
 * Generates JPA-annotated Java entity source files from {@link ClassDetails}
 * using FreeMarker templates.
 *
 * @author Koen Aers
 */
public class EntityExporter implements Exporter {

	private static final String DEFAULT_TEMPLATE_PATH = "/entity";
	private static final String TEMPLATE_NAME = "main.entity.ftl";

	private List<ClassDetails> entities;
	private ModelsContext modelsContext;
	private MetadataHelper metadataHelper;
	private boolean annotated;
	private boolean useGenerics;
	private Configuration freemarkerConfig;
	private Properties customProperties;

	private String templateName;
	private Properties exporterProperties = new Properties();

	public EntityExporter() {}

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
		boolean ejb3 = Boolean.parseBoolean(
				exporterProperties.getProperty("ejb3", "false"));
		boolean jdk5 = Boolean.parseBoolean(
				exporterProperties.getProperty("jdk5", "false"));
		EntityExporter configured = create(md, ejb3, jdk5, templatePath);
		configured.setProperties(exporterProperties);
		configured.exportAll(destDir);
	}

	private EntityExporter(List<ClassDetails> entities, ModelsContext modelsContext,
						   boolean annotated, boolean useGenerics, String[] templatePath) {
		this.entities = entities;
		this.modelsContext = modelsContext;
		this.annotated = annotated;
		this.useGenerics = useGenerics;
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
		this.templateName = resolveTemplateName(templatePath);
	}

	public static EntityExporter create(List<ClassDetails> entities, ModelsContext modelsContext) {
		return new EntityExporter(entities, modelsContext, true, true, new String[0]);
	}

	public static EntityExporter create(List<ClassDetails> entities, ModelsContext modelsContext,
										boolean annotated) {
		return new EntityExporter(entities, modelsContext, annotated, true, new String[0]);
	}

	public static EntityExporter create(List<ClassDetails> entities, ModelsContext modelsContext,
										boolean annotated, String[] templatePath) {
		return new EntityExporter(entities, modelsContext, annotated, true, templatePath);
	}

	public static EntityExporter create(MetadataDescriptor md) {
		return create(md, true, true, new String[0]);
	}

	public static EntityExporter create(MetadataDescriptor md, boolean annotated) {
		return create(md, annotated, true, new String[0]);
	}

	public static EntityExporter create(MetadataDescriptor md, boolean annotated,
										String[] templatePath) {
		return create(md, annotated, true, templatePath);
	}

	public static EntityExporter create(MetadataDescriptor md, boolean annotated,
										boolean useGenerics, String[] templatePath) {
		MetadataHelper helper = MetadataHelper.from(md);
		EntityExporter exporter = new EntityExporter(
				helper.getEntityClassDetails(), helper.getModelsContext(),
				annotated, useGenerics, templatePath);
		exporter.metadataHelper = helper;
		return exporter;
	}

	public void exportAll(File outputDir) {
		ArtifactCollector ac = null;
		if (customProperties != null) {
			ac = (ArtifactCollector) customProperties.get(
					ExporterConstants.ARTIFACT_COLLECTOR);
		}
		for (ClassDetails entity : entities) {
			Map<String, List<String>> classMeta = getClassMeta(entity);
			Map<String, Map<String, List<String>>> fieldMeta = getFieldMeta(entity);
			String outputClassName = resolveOutputClassName(entity, classMeta);
			File outputFile = resolveOutputFile(outputDir, outputClassName);
			outputFile.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(outputFile)) {
				export(writer, entity, classMeta, fieldMeta);
			} catch (Exception e) {
				throw new RuntimeException(
						"Failed to export " + entity.getClassName()
						+ " to " + outputFile, e);
			}
			if (ac != null) {
				ac.addFile(outputFile, "java");
			}
		}
	}

	public void export(Writer output, ClassDetails entity) {
		export(output, entity, getClassMeta(entity), getFieldMeta(entity));
	}

	public void export(Writer output, ClassDetails entity,
					   Map<String, List<String>> classMetaAttributes,
					   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		String packageName = getPackageName(entity);
		// For generated-class, the package may differ
		List<String> generatedClass = classMetaAttributes.getOrDefault(
				"generated-class", Collections.emptyList());
		if (!generatedClass.isEmpty()) {
			String gc = generatedClass.get(0).trim();
			int lastDot = gc.lastIndexOf('.');
			if (lastDot > 0) {
				packageName = gc.substring(0, lastDot);
			}
		}
		ImportContextImpl importContext = new ImportContextImpl(packageName);
		Map<String, Map<String, List<String>>> allClassMeta = getAllClassMeta();
		TemplateHelper templateHelper = new TemplateHelper(
				entity, modelsContext, importContext, annotated, useGenerics,
				classMetaAttributes, fieldMetaAttributes, allClassMeta);
		Map<String, Object> model = new HashMap<>();
		if (customProperties != null) {
			for (String name : customProperties.stringPropertyNames()) {
				model.put(name, customProperties.getProperty(name));
			}
		}
		model.put("templateHelper", templateHelper);
		model.put("date", new Date());
		model.put("version", Version.versionString());
		try {
			Template template = freemarkerConfig.getTemplate(templateName);
			template.process(model, output);
			output.flush();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Failed to export entity: " + entity.getClassName(), e);
		}
	}

	private Map<String, Map<String, List<String>>> getAllClassMeta() {
		if (metadataHelper != null) {
			return metadataHelper.getAllClassMetaAttributes();
		}
		return Collections.emptyMap();
	}

	private Map<String, List<String>> getClassMeta(ClassDetails entity) {
		if (metadataHelper != null) {
			return metadataHelper.getClassMetaAttributes(entity.getClassName());
		}
		return Collections.emptyMap();
	}

	private Map<String, Map<String, List<String>>> getFieldMeta(ClassDetails entity) {
		if (metadataHelper != null) {
			return metadataHelper.getFieldMetaAttributes(entity.getClassName());
		}
		return Collections.emptyMap();
	}

	/**
	 * Resolves the output class name, considering the generated-class
	 * meta attribute which redirects output to a different file.
	 */
	private String resolveOutputClassName(ClassDetails entity,
										  Map<String, List<String>> classMeta) {
		List<String> generatedClass = classMeta.getOrDefault(
				"generated-class", Collections.emptyList());
		if (!generatedClass.isEmpty()) {
			return generatedClass.get(0).trim();
		}
		return entity.getClassName();
	}

	private File resolveOutputFile(File outputDir, String className) {
		int lastDot = className.lastIndexOf('.');
		String simpleName;
		String packagePath;
		if (lastDot >= 0) {
			simpleName = className.substring(lastDot + 1);
			packagePath = className.substring(0, lastDot)
					.replace('.', File.separatorChar);
		} else {
			simpleName = className;
			packagePath = null;
		}
		File dir = (packagePath != null && !packagePath.isEmpty())
				? new File(outputDir, packagePath)
				: outputDir;
		return new File(dir, simpleName + ".java");
	}

	public void setProperties(Properties props) {
		this.customProperties = props;
	}

	public List<ClassDetails> getEntities() {
		return entities;
	}

	private String getPackageName(ClassDetails entity) {
		String className = entity.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	private String resolveTemplateName(String[] templatePath) {
		if (templatePath != null) {
			for (String path : templatePath) {
				File newTemplate = new File(path, TEMPLATE_NAME);
				if (newTemplate.isFile()) {
					return TEMPLATE_NAME;
				}
			}
		}
		return TEMPLATE_NAME;
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
						throw new RuntimeException("Failed to create template loader for: " + path, e);
					}
				}
			}
		}
		loaders.add(new ClassTemplateLoader(getClass().getClassLoader(), DEFAULT_TEMPLATE_PATH));
		return new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0]));
	}
}
