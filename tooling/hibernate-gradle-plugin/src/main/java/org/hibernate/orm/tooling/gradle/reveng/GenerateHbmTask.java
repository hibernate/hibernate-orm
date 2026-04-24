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
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.hbm.HbmXmlExporter;

@Deprecated(forRemoval = true)
@DisableCachingByDefault(because = "Generates output from a live database connection")
public class GenerateHbmTask extends RevengTask {

	@TaskAction
	public void performTask() {
		getLogger().warn( "The generateHbm task is deprecated and will be removed in a future version. "
				+ "Use the generateJava task to generate annotated Java entities instead." );
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating HBM exporter");
		MetadataDescriptor md = createJdbcDescriptor();
		String templatePath = getRevengSpec().templatePath;
		String[] tPath = templatePath != null
				? new String[] { templatePath } : new String[0];
		File outputFolder = getOutputFolder();
		getLogger().lifecycle("Starting HBM export to directory: " + outputFolder + "...");
		HbmXmlExporter.create(md, tPath)
				.exportAll(outputFolder);
		getLogger().lifecycle("HBM export finished");
	}

}
