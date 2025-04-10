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

public class GenerateCfgTask extends AbstractTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating CFG exporter");
		Exporter cfgExporter = ExporterFactory.createExporter(ExporterType.CFG);
		File outputFolder = getOutputFolder();
		cfgExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createJdbcDescriptor());
		cfgExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		getLogger().lifecycle("Starting CFG export to directory: " + outputFolder + "...");
		cfgExporter.start();
		getLogger().lifecycle("CFG export finished");
	}

}
