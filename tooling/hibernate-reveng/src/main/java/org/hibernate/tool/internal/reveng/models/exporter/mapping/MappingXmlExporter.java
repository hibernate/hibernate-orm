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
package org.hibernate.tool.internal.reveng.models.exporter.mapping;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.version.Version;

/**
 * Generates a JPA {@code mapping.xml} file per entity from {@link ClassDetails}
 * using FreeMarker templates.
 *
 * @author Koen Aers
 */
public class MappingXmlExporter {

	private static final String DEFAULT_TEMPLATE_PATH = "/models/mapping";
	private static final String TEMPLATE_NAME = "main.mapping.ftl";

	private final Configuration freemarkerConfig;

	private MappingXmlExporter(String[] templatePath) {
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
	}

	public static MappingXmlExporter create() {
		return new MappingXmlExporter(new String[0]);
	}

	public static MappingXmlExporter create(String[] templatePath) {
		return new MappingXmlExporter(templatePath);
	}

	public void export(Writer output, ClassDetails entity) {
		MappingXmlHelper helper = new MappingXmlHelper(entity);
		Map<String, Object> model = new HashMap<>();
		model.put("helper", helper);
		model.put("date", new Date());
		model.put("version", Version.versionString());
		try {
			Template template = freemarkerConfig.getTemplate(TEMPLATE_NAME);
			template.process(model, output);
			output.flush();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Failed to export mapping.xml for: " + entity.getClassName(), e);
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
