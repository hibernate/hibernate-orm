/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.entity.EntityExporter;

@DisableCachingByDefault(because = "Generates output from a live database connection")
public class GenerateJavaTask extends RevengTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating Java exporter");
		MetadataDescriptor md = createJdbcDescriptor();
		boolean ejb3 = getRevengSpec().generateAnnotations;
		boolean generics = getRevengSpec().useGenerics;
		String templatePath = getRevengSpec().templatePath;
		String[] tPath = templatePath != null
				? new String[] { templatePath } : new String[0];
		File outputFolder = getOutputFolder();
		getLogger().lifecycle("Starting Java export to directory: " + outputFolder + "...");
		EntityExporter.create(md, ejb3, generics, tPath)
				.exportAll(outputFolder);
		getLogger().lifecycle("Java export finished");
	}

}
