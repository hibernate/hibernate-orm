package org.hibernate.tool.gradle.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.gradle.api.tasks.TaskAction;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;

public class GenerateJavaTask extends AbstractTask {

	private Properties hibernateProperties = null;
	
	private Properties getHibernateProperties() {
		if (hibernateProperties == null) {
			loadPropertiesFile(getPropertyFile());
		}
		return hibernateProperties;
	}
	
	private File getPropertyFile() {
		return new File(getProject().getProjectDir(), "src/main/resources/hibernate.properties");
	}

	private void loadPropertiesFile(File propertyFile) {
		getLogger().lifecycle("Loading the properties file : " + propertyFile.getPath());
		try (FileInputStream is = new FileInputStream(propertyFile)) {
			hibernateProperties = new Properties();
			hibernateProperties.load(is);
			getLogger().lifecycle("Properties file is loaded");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new BuildException(propertyFile + " not found.", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new BuildException("Problem while loading " + propertyFile, e);
		}
	}
	
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
		return new File(getProject().getProjectDir(), "generated-sources");
	}
	
	private RevengStrategy setupReverseEngineeringStrategy() {
		return new DefaultStrategy();
	}

}
