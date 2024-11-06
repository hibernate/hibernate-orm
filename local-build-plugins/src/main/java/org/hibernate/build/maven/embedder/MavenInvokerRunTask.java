package org.hibernate.build.maven.embedder;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public abstract class MavenInvokerRunTask extends DefaultTask {

	@ServiceReference
	abstract Property<MavenEmbedderService> getMavenEmbedderService();

	@TaskAction
	public void runInvoker() {
		// install the plugin artefact
		getMavenEmbedderService().get().execute( constructTaskAndArgs() );
	//	getMavenEmbedderService().get().execute( "invoker:run");
		getMavenEmbedderService().get().execute( "integration-test" );
	}

	private String[] constructTaskAndArgs() {
		ArrayList<String> taskAndArgs = new ArrayList<String>();
		taskAndArgs.add("install:install-file");
		taskAndArgs.add("-Dpackaging=jar");
		taskAndArgs.add("-DartifactId=" + getProject().getName());
		taskAndArgs.add("-DlocalRepositoryPath=" + getPathToLocalRepository());
		taskAndArgs.add("-Dfile=" + getPathToArtifact());
		taskAndArgs.add("-DpomFile=" + getPathToPomFile());
		return taskAndArgs.toArray(new String[0]);
	}

	private String getPathToArtifact() {
		Directory buildDir = getProject().getLayout().getBuildDirectory().get();
		return buildDir
				.dir( "libs" )
				.file( getArtifactName())
				.getAsFile()
				.getAbsolutePath();
	}

	private String getArtifactName() {
		return getProject().getName() + "-" + getProject().getVersion() + ".jar";
	}

	private String getPathToPomFile() {
		return new File(getProject().getProjectDir(), "pom.xml").getAbsolutePath();
	}

	private String getPathToLocalRepository() {
		return getMavenEmbedderService()
				.get()
				.getParameters()
				.getMavenLocalDirectory()
				.getAsFile()
				.get()
				.getAbsolutePath();
	}


}
