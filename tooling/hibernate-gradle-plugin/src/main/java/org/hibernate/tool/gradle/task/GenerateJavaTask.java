package org.hibernate.tool.gradle.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
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

public class GenerateJavaTask extends DefaultTask {

	@TaskAction
	public void performTask() {
		getLogger().lifecycle("Starting Task 'GenerateJavaTask'");
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					new URLClassLoader(
							resolveProjectClassPath(), 
							oldLoader));
			generate();
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
			getLogger().lifecycle("Ending Task 'GenerateJavaTask");
		}
	}

	private RevengStrategy setupReverseEngineeringStrategy() {
		return new DefaultStrategy();
	}

	private MetadataDescriptor createJdbcDescriptor(RevengStrategy strategy, Properties properties) {
		properties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, true);
		return MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, properties);
	}

	private File getPropertyFile() {
		return new File(getProject().getProjectDir(), "src/main/resources/hibernate.properties");
	}

	private Properties loadPropertiesFile(File propertyFile) {
		try (FileInputStream is = new FileInputStream(propertyFile)) {
			Properties result = new Properties();
			result.load(is);
			return result;
		} catch (FileNotFoundException e) {
			throw new BuildException(propertyFile + " not found.", e);
		} catch (IOException e) {
			throw new BuildException("Problem while loading " + propertyFile, e);
		}
	}

	private File getOutputFolder() {
		return new File(getProject().getProjectDir(), "generated-sources");
	}

	private void executeExporter(MetadataDescriptor mdd) {
		getLogger().lifecycle("Creating POJO exporter");
		Exporter pojoExporter = ExporterFactory.createExporter(ExporterType.JAVA);
		File outputFolder = getOutputFolder();
		pojoExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, mdd);
		pojoExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		getLogger().lifecycle("Starting POJO export to directory: " + outputFolder + "...");
		pojoExporter.start();
		getLogger().lifecycle("POJO export finished");
	}
	
	private URL[] resolveProjectClassPath() {
		try {
			ConfigurationContainer cc = getProject().getConfigurations();
			Configuration defaultConf = cc.getByName("compileClasspath");
			ResolvedConfiguration resolvedConf = defaultConf.getResolvedConfiguration();
			Set<ResolvedArtifact> ras = resolvedConf.getResolvedArtifacts();
			ResolvedArtifact[] resolvedArtifacts = ras.toArray(new ResolvedArtifact[ras.size()]);
			URL[] urls = new URL[ras.size()];
			for (int i = 0; i < ras.size(); i++) {
				urls[i] = resolvedArtifacts[i].getFile().toURI().toURL();
			}
			return urls;
		} catch (MalformedURLException e) {
			getLogger().error("MalformedURLException while compiling project classpath");
			throw new RuntimeException(e);
		}
	}
	
	private void generate() {
		File propertyFile = getPropertyFile();
		if (propertyFile.exists()) {
			executeExporter(
					createJdbcDescriptor(
							setupReverseEngineeringStrategy(), 
							loadPropertiesFile(propertyFile)));
		} else {
			getLogger().lifecycle("Property file '" + propertyFile + "' cannot be found, aborting...");
		}		
	}

}
