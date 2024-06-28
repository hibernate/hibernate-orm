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
