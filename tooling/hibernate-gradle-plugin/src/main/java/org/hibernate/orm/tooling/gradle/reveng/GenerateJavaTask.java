/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

@DisableCachingByDefault(because = "Reverse engineering tasks perform JDBC operations and are not cacheable")
public class GenerateJavaTask extends RevengTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating Java exporter");
		Exporter pojoExporter = ExporterFactory.createExporter(ExporterType.JAVA);
		pojoExporter.getProperties().setProperty("ejb3", String.valueOf(getRevengSpec().generateAnnotations));
		pojoExporter.getProperties().setProperty("jdk5", String.valueOf(getRevengSpec().useGenerics));
		File outputFolder = getOutputFolder();
		pojoExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createJdbcDescriptor());
		pojoExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		String templatePath = getRevengSpec().templatePath;
		if (templatePath != null) {
			getLogger().lifecycle("Setting template path to: " + templatePath);
			pojoExporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[] { templatePath });
		}
		getLogger().lifecycle("Starting Java export to directory: " + outputFolder + "...");
		pojoExporter.start();
		getLogger().lifecycle("Java export finished");
	}

}
