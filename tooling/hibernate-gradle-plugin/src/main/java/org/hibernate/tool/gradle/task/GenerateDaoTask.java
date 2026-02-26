/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.gradle.task;

import java.io.File;

import org.gradle.api.tasks.TaskAction;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

public class GenerateDaoTask extends AbstractTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating DAO exporter");
		Exporter hbmExporter = ExporterFactory.createExporter(ExporterType.DAO);
		File outputFolder = getOutputFolder();
		hbmExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createJdbcDescriptor());
		hbmExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		String templatePath = getExtension().templatePath;
		if (templatePath != null) {
			getLogger().lifecycle("Setting template path to: " + templatePath);
			hbmExporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[] { templatePath });
		}
		getLogger().lifecycle("Starting DAO export to directory: " + outputFolder + "...");
		hbmExporter.start();
		getLogger().lifecycle("DAO export finished");
	}

}
