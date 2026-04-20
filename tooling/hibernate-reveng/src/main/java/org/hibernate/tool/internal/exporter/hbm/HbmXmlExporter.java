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
package org.hibernate.tool.internal.exporter.hbm;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import jakarta.persistence.Embeddable;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.export.ArtifactCollector;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.util.MetadataHelper;
import org.hibernate.tool.api.version.Version;
import org.hibernate.tool.internal.util.EntityFileWriter;

import java.util.Properties;

/**
 * Generates Hibernate {@code hbm.xml} mapping files per entity from
 * {@link ClassDetails} using FreeMarker templates.
 *
 * @author Koen Aers
 */
public class HbmXmlExporter implements Exporter {

	private static final String DEFAULT_TEMPLATE_PATH = "/hbm";
	private static final String TEMPLATE_NAME = "main.hbm.ftl";

	private Configuration freemarkerConfig;
	private HibernateMappingSettings mappingSettings;
	private List<ClassDetails> entities;
	private MetadataHelper metadataHelper;
	private Properties exporterProperties = new Properties();

	public HbmXmlExporter() {}

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
		HibernateMappingSettings settings = (HibernateMappingSettings)
				exporterProperties.get(MAPPING_SETTINGS);
		if (settings == null) settings = HibernateMappingSettings.defaults();
		HbmXmlExporter configured = new HbmXmlExporter(templatePath, settings);
		MetadataHelper helper = MetadataHelper.from(md);
		configured.entities = helper.getEntityClassDetails();
		configured.metadataHelper = helper;
		configured.exporterProperties = exporterProperties;
		configured.exportAll(destDir);
	}

	/**
	 * Property key for passing {@link HibernateMappingSettings} via the
	 * {@link Exporter} properties map.
	 */
	public static final String MAPPING_SETTINGS =
			"org.hibernate.tool.hbm.mapping_settings";

	private HbmXmlExporter(String[] templatePath, HibernateMappingSettings mappingSettings) {
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
		this.mappingSettings = mappingSettings;
	}

	public static HbmXmlExporter create() {
		return new HbmXmlExporter(new String[0], HibernateMappingSettings.defaults());
	}

	public static HbmXmlExporter create(HibernateMappingSettings mappingSettings) {
		return new HbmXmlExporter(new String[0], mappingSettings);
	}

	public static HbmXmlExporter create(String[] templatePath) {
		return new HbmXmlExporter(templatePath, HibernateMappingSettings.defaults());
	}

	public static HbmXmlExporter create(String[] templatePath, HibernateMappingSettings mappingSettings) {
		return new HbmXmlExporter(templatePath, mappingSettings);
	}

	public static HbmXmlExporter create(MetadataDescriptor md) {
		return create(md, new String[0]);
	}

	public static HbmXmlExporter create(MetadataDescriptor md, String[] templatePath) {
		HbmXmlExporter exporter = new HbmXmlExporter(templatePath, HibernateMappingSettings.defaults());
		MetadataHelper helper = MetadataHelper.from(md);
		exporter.entities = helper.getEntityClassDetails();
		exporter.metadataHelper = helper;
		return exporter;
	}

	public void exportAll(File outputDir) {
		if (entities == null) {
			throw new IllegalStateException(
					"exportAll() requires creation via create(MetadataDescriptor, ...)");
		}
		ArtifactCollector ac = (ArtifactCollector)
				exporterProperties.get(ExporterConstants.ARTIFACT_COLLECTOR);
		for (ClassDetails cd : entities) {
			if (cd.hasDirectAnnotationUsage(Embeddable.class)) {
				continue;
			}
			File outputFile = EntityFileWriter.resolveOutputFile(
					outputDir, cd.getClassName(), ".hbm.xml");
			outputFile.getParentFile().mkdirs();
			try (Writer writer = new java.io.FileWriter(outputFile)) {
				Map<String, List<String>> classMeta = metadataHelper != null
						? metadataHelper.getClassMetaAttributes(cd.getClassName())
						: Collections.emptyMap();
				Map<String, Map<String, List<String>>> fieldMeta = metadataHelper != null
						? metadataHelper.getFieldMetaAttributes(cd.getClassName())
						: Collections.emptyMap();
				String comment = classMeta.containsKey("hibernate.comment")
						? classMeta.get("hibernate.comment").get(0) : null;
				Map<String, Map<String, List<String>>> allClassMeta = metadataHelper != null
						? metadataHelper.getAllClassMetaAttributes()
						: Collections.emptyMap();
				export(writer, cd, comment, classMeta, Collections.emptyMap(),
						fieldMeta, allClassMeta);
			} catch (Exception e) {
				throw new RuntimeException(
						"Failed to export hbm.xml for: " + cd.getClassName(), e);
			}
			if (ac != null) {
				ac.addFile(outputFile, "hbm.xml");
			}
		}
		if (ac != null) {
			ac.formatFiles();
		}
	}

	public void export(Writer output, ClassDetails entity) {
		export(output, entity, null, Collections.emptyMap());
	}

	public void export(Writer output, ClassDetails entity, String comment,
					   Map<String, List<String>> metaAttributes) {
		export(output, entity, comment, metaAttributes, Collections.emptyMap());
	}

	public void export(Writer output, ClassDetails entity, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports) {
		export(output, entity, comment, metaAttributes, imports, Collections.emptyMap());
	}

	public void export(Writer output, ClassDetails entity, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports,
					   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		export(output, entity, comment, metaAttributes, imports,
				fieldMetaAttributes, Collections.emptyMap());
	}

	public void export(Writer output, ClassDetails entity, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports,
					   Map<String, Map<String, List<String>>> fieldMetaAttributes,
					   Map<String, Map<String, List<String>>> allClassMetaAttributes) {
		HbmTemplateHelper helper = new HbmTemplateHelper(
				entity, comment, metaAttributes, imports,
				fieldMetaAttributes, allClassMetaAttributes);
		Map<String, Object> model = new HashMap<>();
		model.put("helper", helper);
		model.put("settings", mappingSettings);
		model.put("date", new Date());
		model.put("version", Version.versionString());
		try {
			Template template = freemarkerConfig.getTemplate(TEMPLATE_NAME);
			template.process(model, output);
			output.flush();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Failed to export hbm.xml for: " + entity.getClassName(), e);
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
						throw new RuntimeException("Failed to create template loader for: " + path, e);
					}
				}
			}
		}
		loaders.add(new ClassTemplateLoader(getClass().getClassLoader(), DEFAULT_TEMPLATE_PATH));
		return new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0]));
	}
}
