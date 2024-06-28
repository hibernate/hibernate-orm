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
		File outputFolder = getOutputFolder();
		pojoExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createJdbcDescriptor());
		pojoExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		getLogger().lifecycle("Starting Java export to directory: " + outputFolder + "...");
		pojoExporter.start();
		getLogger().lifecycle("Java export finished");
	}

}
