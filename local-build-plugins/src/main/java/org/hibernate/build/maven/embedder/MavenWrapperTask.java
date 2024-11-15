package org.hibernate.build.maven.embedder;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

public abstract class MavenWrapperTask extends DefaultTask {

	@ServiceReference
	abstract Property<MavenEmbedderService> getMavenEmbedderService();

	@InputDirectory
	abstract DirectoryProperty getIntegrationTestSourcesFolder();

	@TaskAction
	public void runInvoker() {
		getMavenEmbedderService().get().execute( constructTaskAndArgs());
	}

	private String[] constructTaskAndArgs() {
		return new String[] {
				"wrapper:wrapper",
				"-f" + getIntegrationTestSourcesFolder().get().getAsFile().getAbsolutePath()
		};
	}

}
