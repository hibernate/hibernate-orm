package org.hibernate.tool.gradle.task;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.tasks.Internal;
import org.hibernate.tool.gradle.Extension;

public abstract class AbstractTask extends DefaultTask {

	@Internal
	private Extension extension = null;
	
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
			throw new RuntimeException(e);
		}
	}
	
	abstract void doWork();

}
