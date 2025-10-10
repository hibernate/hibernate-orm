/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.gradle.task;

import java.io.File;

import org.gradle.api.tasks.TaskAction;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;

public class GenerateJavaTask extends AbstractTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating Java exporter");
		Exporter pojoExporter = ExporterFactory.createExporter(ExporterType.JAVA);
        pojoExporter.getProperties().setProperty("ejb3", String.valueOf(getExtension().generateAnnotations));
		pojoExporter.getProperties().setProperty("jdk5", String.valueOf(getExtension().useGenerics));
		File outputFolder = getOutputFolder();
		pojoExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createJdbcDescriptor());
		pojoExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
        String templatePath = getExtension().templatePath;
        if (templatePath != null) {
            getLogger().lifecycle("Setting template path to: " + templatePath);
            pojoExporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[] { templatePath });
        }
		getLogger().lifecycle("Starting Java export to directory: " + outputFolder + "...");
		pojoExporter.start();
		getLogger().lifecycle("Java export finished");
	}
	
}
