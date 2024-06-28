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
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategyFactory;
import org.hibernate.tool.gradle.Extension;

public abstract class AbstractTask extends DefaultTask {

	@Internal
	private Extension extension = null;
	
	@Internal
	private Properties hibernateProperties = null;
	
	public void initialize(Extension extension) {
		this.extension = extension;
	}
	
	Extension getExtension() {
		return this.extension;
	}
	
	void perform() {
		getLogger().lifecycle("Starting Task '" + getName() + "'");
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					new URLClassLoader(
							resolveProjectClassPath(), 
							oldLoader));
			doWork();
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
			getLogger().lifecycle("Ending Task '" + getName() + "'");
		}
	}
	
	URL[] resolveProjectClassPath() {
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
			throw new BuildException(e);
		}
	}
	
	Properties getHibernateProperties() {
		if (hibernateProperties == null) {
			loadPropertiesFile(getPropertyFile());
		}
		return hibernateProperties;
	}
	
	String getHibernateProperty(String name) {
		return getHibernateProperties().getProperty(name);
	}
	
	MetadataDescriptor createJdbcDescriptor() {
		RevengStrategy strategy = setupReverseEngineeringStrategy();
		Properties hibernateProperties = getHibernateProperties();
		hibernateProperties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, true);
		return MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, hibernateProperties);
	}

	@Internal
	File getOutputFolder() {
		return new File(getProject().getProjectDir(), getExtension().outputFolder);
	}
	
	RevengStrategy setupReverseEngineeringStrategy() {
		RevengStrategy result = RevengStrategyFactory
				.createReverseEngineeringStrategy(getExtension().revengStrategy);
		RevengSettings settings = new RevengSettings(result);
		settings.setDefaultPackageName(getExtension().packageName);
		result.setSettings(settings);
		return result;
	}
	
	private File getPropertyFile() {
		String hibernatePropertiesFile = getExtension().hibernateProperties;
		SourceSetContainer ssc = getProject().getExtensions().getByType(SourceSetContainer.class);
		SourceSet ss = ssc.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		SourceDirectorySet sds = ss.getResources();
		for (File f : sds.getFiles()) {
			if (hibernatePropertiesFile.equals(f.getName())) {
				return f;
			}
		}
		throw new BuildException("File '" + hibernatePropertiesFile + "' could not be found");
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
	
	abstract void doWork();

}
