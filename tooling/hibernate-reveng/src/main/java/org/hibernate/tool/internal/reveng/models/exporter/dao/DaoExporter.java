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
package org.hibernate.tool.internal.reveng.models.exporter.dao;

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
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.api.version.Version;
import org.hibernate.tool.internal.export.java.ImportContextImpl;

/**
 * Generates DAO Home Java source files from {@link ClassDetails}
 * using FreeMarker templates.
 *
 * @author Koen Aers
 */
public class DaoExporter {

	private static final String DEFAULT_TEMPLATE_PATH = "/models/dao";
	private static final String TEMPLATE_NAME = "main.dao.ftl";

	private final List<ClassDetails> entities;
	private final ModelsContext modelsContext;
	private final boolean ejb3;
	private final String sessionFactoryName;
	private final Configuration freemarkerConfig;

	private DaoExporter(List<ClassDetails> entities, ModelsContext modelsContext,
						boolean ejb3, String sessionFactoryName, String[] templatePath) {
		this.entities = entities;
		this.modelsContext = modelsContext;
		this.ejb3 = ejb3;
		this.sessionFactoryName = sessionFactoryName;
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
	}

	public static DaoExporter create(List<ClassDetails> entities, ModelsContext modelsContext) {
		return new DaoExporter(entities, modelsContext, true, "SessionFactory", new String[0]);
	}

	public static DaoExporter create(List<ClassDetails> entities, ModelsContext modelsContext,
									 boolean ejb3) {
		return new DaoExporter(entities, modelsContext, ejb3, "SessionFactory", new String[0]);
	}

	public static DaoExporter create(List<ClassDetails> entities, ModelsContext modelsContext,
									 boolean ejb3, String sessionFactoryName) {
		return new DaoExporter(entities, modelsContext, ejb3, sessionFactoryName, new String[0]);
	}

	public static DaoExporter create(List<ClassDetails> entities, ModelsContext modelsContext,
									 boolean ejb3, String sessionFactoryName,
									 String[] templatePath) {
		return new DaoExporter(entities, modelsContext, ejb3, sessionFactoryName, templatePath);
	}

	public void export(Writer output, ClassDetails entity) {
		String packageName = getPackageName(entity);
		ImportContextImpl importContext = new ImportContextImpl(packageName);
		DaoTemplateHelper helper = new DaoTemplateHelper(
				entity, importContext, ejb3, sessionFactoryName);
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
					"Failed to export DAO for entity: " + entity.getClassName(), e);
		}
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
		loaders.add(new ClassTemplateLoader(getClass().getClassLoader(), DEFAULT_TEMPLATE_PATH));
		return new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0]));
	}
}
