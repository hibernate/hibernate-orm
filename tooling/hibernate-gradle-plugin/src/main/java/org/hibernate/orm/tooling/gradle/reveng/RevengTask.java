/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

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
import org.gradle.work.DisableCachingByDefault;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.api.reveng.RevengStrategyFactory;


@DisableCachingByDefault(because = "Generates output from a live database connection")
public abstract class RevengTask extends DefaultTask {

	@Internal
	private RevengSpec revengSpec = null;

	@Internal
	private Properties hibernateProperties = null;

	public void initialize(RevengSpec revengSpec) {
		this.revengSpec = revengSpec;
	}

	RevengSpec getRevengSpec() {
		return this.revengSpec;
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
		}
		finally {
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
		}
		catch (MalformedURLException e) {
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
		return new File(getProject().getProjectDir(), getRevengSpec().outputFolder);
	}

	RevengStrategy setupReverseEngineeringStrategy() {
		File[] revengFiles = getRevengFiles();
		RevengStrategy result = RevengStrategyFactory
				.createReverseEngineeringStrategy(getRevengSpec().revengStrategy, revengFiles);
		RevengSettings settings = new RevengSettings(result);
		settings.setDefaultPackageName(getRevengSpec().packageName);
		result.setSettings(settings);
		return result;
	}

	private File getFile(String filename) {
		SourceSetContainer ssc = getProject().getExtensions().getByType(SourceSetContainer.class);
		SourceSet ss = ssc.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		SourceDirectorySet sds = ss.getResources();
		for (File f : sds.getFiles()) {
			if (filename.equals(f.getName())) {
				return f;
			}
		}
		throw new BuildException("File '" + filename + "' could not be found");
	}

	private File getPropertyFile() {
		return getFile(getRevengSpec().hibernateProperties);
	}

	private File[] getRevengFiles() {
		String revengFile = getRevengSpec().revengFile;
		if (revengFile == null) {
			return null;
		}

		return new File[] { getFile(revengFile) };
	}

	private void loadPropertiesFile(File propertyFile) {
		getLogger().lifecycle("Loading the properties file : " + propertyFile.getPath());
		try (FileInputStream is = new FileInputStream(propertyFile)) {
			hibernateProperties = new Properties();
			hibernateProperties.load(is);
			getLogger().lifecycle("Properties file is loaded");
		}
		catch (FileNotFoundException e) {
			throw new BuildException(propertyFile + " not found.", e);
		}
		catch (IOException e) {
			throw new BuildException("Problem while loading " + propertyFile, e);
		}
	}

	abstract void doWork();

}
