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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.hibernate.tool.internal.export.java.ImportContextImpl;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Generates JPA-annotated Java entity source files from {@link TableMetadata}
 * using FreeMarker templates.
 *
 * @author Koen Aers
 */
public class ModelsJavaExporter {

	private final List<TableMetadata> tables;
	private final Configuration freemarkerConfig;

	private ModelsJavaExporter(List<TableMetadata> tables) {
		this.tables = tables;
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setClassLoaderForTemplateLoading(
				getClass().getClassLoader(), "/");
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
	}

	public static ModelsJavaExporter create(List<TableMetadata> tables) {
		return new ModelsJavaExporter(tables);
	}

	public void export(Writer output, TableMetadata table) {
		ImportContextImpl importContext = new ImportContextImpl(table.getEntityPackage());
		EntityTemplateHelper pojo = new EntityTemplateHelper(table, importContext);
		Map<String, Object> model = new HashMap<>();
		model.put("pojo", pojo);
		try {
			Template template = freemarkerConfig.getTemplate("models/entity/Entity.ftl");
			template.process(model, output);
			output.flush();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException("Failed to export entity: " + table.getEntityClassName(), e);
		}
	}
}
