/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
