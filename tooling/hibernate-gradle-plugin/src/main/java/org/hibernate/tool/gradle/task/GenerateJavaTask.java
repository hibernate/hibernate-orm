package org.hibernate.tool.gradle.task;

import java.io.File;
import java.util.Properties;

import org.gradle.api.tasks.TaskAction;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategyFactory;

public class GenerateJavaTask extends AbstractTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating POJO exporter");
		Exporter pojoExporter = ExporterFactory.createExporter(ExporterType.JAVA);
		File outputFolder = getOutputFolder();
		pojoExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createJdbcDescriptor());
		pojoExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		getLogger().lifecycle("Starting POJO export to directory: " + outputFolder + "...");
		pojoExporter.start();
		getLogger().lifecycle("POJO export finished");
	}

	private MetadataDescriptor createJdbcDescriptor() {
		RevengStrategy strategy = setupReverseEngineeringStrategy();
		Properties hibernateProperties = getHibernateProperties();
		hibernateProperties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, true);
		return MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, hibernateProperties);
	}

	private File getOutputFolder() {
		return new File(getProject().getProjectDir(), getExtension().outputFolder);
	}
	
	private RevengStrategy setupReverseEngineeringStrategy() {
		RevengStrategy result = RevengStrategyFactory
				.createReverseEngineeringStrategy(getExtension().revengStrategy);
		RevengSettings settings = new RevengSettings(result);
		settings.setDefaultPackageName(getExtension().packageName);
		result.setSettings(settings);
		return result;
	}
	
}
