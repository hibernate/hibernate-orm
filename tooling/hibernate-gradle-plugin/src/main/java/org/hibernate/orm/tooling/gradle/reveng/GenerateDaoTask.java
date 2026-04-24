/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.dao.DaoExporter;

@DisableCachingByDefault(because = "Generates output from a live database connection")
public class GenerateDaoTask extends RevengTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating DAO exporter");
		MetadataDescriptor md = createJdbcDescriptor();
		boolean ejb3 = getRevengSpec().generateAnnotations;
		String templatePath = getRevengSpec().templatePath;
		String[] tPath = templatePath != null
				? new String[] { templatePath } : new String[0];
		File outputFolder = getOutputFolder();
		getLogger().lifecycle("Starting DAO export to directory: " + outputFolder + "...");
		DaoExporter.create(md, ejb3, "SessionFactory", tPath)
				.exportAll(outputFolder);
		getLogger().lifecycle("DAO export finished");
	}

}
