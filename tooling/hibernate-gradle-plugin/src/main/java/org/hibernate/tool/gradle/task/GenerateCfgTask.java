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
import java.io.FileWriter;
import java.io.Writer;
import java.util.Properties;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.exporter.cfg.CfgXmlExporter;

@DisableCachingByDefault(because = "Generates output from a live database connection")
public class GenerateCfgTask extends AbstractTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating CFG exporter");
		MetadataDescriptor md = createJdbcDescriptor();
		File outputFolder = getOutputFolder();
		File outputFile = new File(outputFolder, "hibernate.cfg.xml");
		outputFile.getParentFile().mkdirs();
		Properties props = new Properties();
		props.putAll(getHibernateProperties());
		getLogger().lifecycle("Starting CFG export to directory: " + outputFolder + "...");
		try (Writer writer = new FileWriter(outputFile)) {
			CfgXmlExporter.create(md)
					.export(writer, props);
		} catch (Exception e) {
			throw new RuntimeException(
					"Failed to export hibernate.cfg.xml to "
					+ outputFile, e);
		}
		getLogger().lifecycle("CFG export finished");
	}

}
