/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.maven.embedder;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin for integrating Maven Embedder into the Gradle build to execute
 * some Maven tasks/goals/mojos.
 *
 * @author Steve Ebersole
 */
public class MavenEmbedderPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// be sure to mirror the output dirs
		project.getLayout().getBuildDirectory().set( project.getLayout().getProjectDirectory().dir( "target" ) );

		final BuildServiceRegistry sharedServices = project.getGradle().getSharedServices();

		// add a DSL extension for configuration
		final MavenEmbedderConfig dsl = project.getExtensions().create(
				"mavenEmbedder",
				MavenEmbedderConfig.class
		);

		final Provider<Directory> workingDirectory = project.getLayout().getBuildDirectory().dir("maven-embedder/workspace");

		// add the MavenEmbedderService shared-build-service
		final Provider<MavenEmbedderService> embedderServiceProvider = sharedServices.registerIfAbsent(
				"maven-embedder",
				MavenEmbedderService.class, (spec) -> {
					spec.getParameters().getProjectVersion().set( project.getVersion().toString() );
					spec.getParameters().getWorkingDirectory().set( workingDirectory );
					spec.getParameters().getMavenLocalDirectory().set( dsl.getLocalRepositoryDirectory() );
				}
		);

		project.getTasks().register(
				"installHibernateCore",
				RunMavenTask.class,
				(task) -> {
					configureInstallHibernateCoreTask(
							configureInstallTask( configureRunMavenTask( task, embedderServiceProvider ) ) );
				});

		project.getTasks().register(
				"installHibernateScanJandex",
				RunMavenTask.class,
				(task) -> {
					configureInstallHibernateScanJandexTask(
							configureInstallTask( configureRunMavenTask( task, embedderServiceProvider ) ));
				} );

		// Via the plugin's POM, we tell Maven to generate the descriptors into
		// `target/generated/sources/plugin-descriptors/META-INF/maven`.
		// `META-INF/maven` is the relative path we need inside the jar, so we
		// configure the "resource directory" in Gradle to be just the
		// `target/generated/sources/plugin-descriptors` part.
		final Provider<Directory> descriptorsDir = project.getLayout().getBuildDirectory().dir( "generated/sources/plugin-descriptors" );



		// create the "mirror" task which calls the appropriate Maven tasks/goals/mojos behind the scenes using the embedder service
		final TaskProvider<MavenPluginDescriptorTask> generatePluginDescriptorTask = project.getTasks().register( "generatePluginDescriptor", MavenPluginDescriptorTask.class, (task) -> {
			task.setGroup( "maven embedder" );

			task.getMavenEmbedderService().set( embedderServiceProvider );
			task.usesService( embedderServiceProvider );

			// deal with the "descriptor directory" -
			//		1. we need this on the Gradle side for up-to-date checking, etc
			task.getDescriptorDirectory().set( descriptorsDir );
			//		2. add the resource dir to the main source-set's resources so that it is picked up for jar
			final SourceSetContainer sourceSets = project.getExtensions().getByType( SourceSetContainer.class );
			final SourceSet mainSourceSet = sourceSets.getByName( "main" );
			mainSourceSet.getResources().srcDir( task.getDescriptorDirectory() );

			// the hibernate-core jar needs to be present in the local repository
			// we need compilation to happen before we generate the descriptors
			task.dependsOn( "prepareWorkspace", "installHibernateCore", "installHibernateScanJandex");
		} );

		project.getTasks().register(
				"createMavenWrapper",
				RunMavenTask.class,
				(task) -> {
					configuraCreateMavenWrapperTask(
							configureRunMavenTask( task, embedderServiceProvider ));
				} );

		project.getTasks().register(
				"installHibernateMavenPlugin",
				RunMavenTask.class,
				(task) -> {
					configureInstallHibernateMavenPluginTask(
							configureInstallTask(
									configureRunMavenTask( task, embedderServiceProvider )));
				} );

		project.getTasks().register(
				"integrationTest",
				RunMavenTask.class,
				(task) -> {
					configureIntegrationTestTask(
							configureRunMavenTask( task, embedderServiceProvider ));
				} );

		// we need the descriptor generation to happen before we jar
		project.getTasks().named( "jar", (jarTask) -> jarTask.dependsOn( generatePluginDescriptorTask ) );
		project.getTasks().named( "check" , (checkTask) -> checkTask.dependsOn( "integrationTest", generatePluginDescriptorTask ) );
	}

	private void configureInstallHibernateMavenPluginTask(RunMavenTask task) {
		List<String> arguments = new ArrayList<String>(task.getArguments().get());
		arguments.add("-Dfile=" + getHibernateMavenPluginArtifactFilePath( task.getProject() ));
		arguments.add("-DartifactId=hibernate-maven-plugin");
		arguments.add( "-DpomFile=" + getHibernateMavenPluginPomFilePath( task.getProject() ) );
		task.getArguments().set( arguments );
		task.dependsOn("jar", "generatePluginDescriptor");
	}

	private void configureInstallHibernateCoreTask(RunMavenTask task) {
		List<String> arguments = new ArrayList<String>(task.getArguments().get());
		arguments.add("-Dfile=" + getHibernateCoreArtifactFilePath( task.getProject() ));
		arguments.add("-DartifactId=hibernate-core");
		arguments.add( "-DpomFile=" + getHibernateCorePomFilePath( task.getProject() ) );
		task.getArguments().set( arguments );
		task.dependsOn(":hibernate-core:generatePomFileForPublishedArtifactsPublication", ":hibernate-core:jar");
	}

	private void configureInstallHibernateScanJandexTask(RunMavenTask task) {
		List<String> arguments = new ArrayList<String>(task.getArguments().get());
		arguments.add("-Dfile=" + getHibernateScanJandexArtifactFilePath( task.getProject() ));
		arguments.add("-DartifactId=hibernate-scan-jandex");
		task.getArguments().set( arguments );
		task.dependsOn(":hibernate-scan-jandex:jar");
	}

	private void configureIntegrationTestTask(RunMavenTask task) {
		task.getGoals().set( "invoker:run" );
		task.dependsOn("createMavenWrapper", "installHibernateMavenPlugin");
	}

	private void configuraCreateMavenWrapperTask(RunMavenTask task) {
		task.getGoals().set("wrapper:wrapper");
		task.getArguments().set( List.of("-f" + getIntegrationTestFolderPath( task.getProject() ) ));
		task.dependsOn( "prepareWorkspace" );
	}

	private String getHibernateMavenPluginPomFilePath(Project project) {
		return project
				.getLayout()
				.getBuildDirectory()
				.file( "publications/publishedArtifacts/pom-default.xml")
				.get()
				.getAsFile()
				.getAbsolutePath();
	}

	private String getHibernateMavenPluginArtifactFilePath(Project project) {
		final String artifactName = "hibernate-maven-plugin-" + project.getVersion() + ".jar";
		final File libsFolder = project.getLayout().getBuildDirectory().dir("libs" ).get().getAsFile();
		return new File(libsFolder, artifactName).getAbsolutePath();
	}

	private RunMavenTask configureRunMavenTask(
			RunMavenTask task,
			Provider<MavenEmbedderService> embedderServiceProvider) {
		task.setGroup( "maven embedder" );
		task.getMavenEmbedderService().set( embedderServiceProvider );
		task.usesService( embedderServiceProvider );
		return task;
	}

	private Directory getWorkingDirectory(Project project) {
		return project.getLayout().getBuildDirectory().dir("maven-embedder/workspace").get();
	}

	private String getIntegrationTestFolderPath(Project project) {
		return getWorkingDirectory( project).dir( "src/it/enhance" ).getAsFile().getAbsolutePath();
	}

	private RunMavenTask configureInstallTask(RunMavenTask task) {
		task.getGoals().set( "install:install-file" );
		ArrayList<String> arguments = new ArrayList<String>();
		arguments.add("-DgroupId=" + task.getProject().getGroup().toString());
		arguments.add("-Dversion=" + task.getProject().getVersion());
		arguments.add("-Dpackaging=jar");
		task.getArguments().set( arguments );
		return task;
	}

	private String getHibernateCoreArtifactFilePath(Project project) {
		final String artifactName = "hibernate-core-" + project.getVersion() + ".jar";
		final File hibernateCoreLibsFolder = getHibernateCoreBuildDirectory( project )
				.dir( "libs" )
				.getAsFile();
		return new File(hibernateCoreLibsFolder, artifactName).getAbsolutePath();
	}

	private String getHibernateCorePomFilePath(Project project) {
		return getHibernateCoreBuildDirectory( project )
				.file( "publications/publishedArtifacts/pom-default.xml" )
				.getAsFile()
				.getAbsolutePath();
	}

	private String getHibernateScanJandexArtifactFilePath(Project project) {
		final String artifactName = "hibernate-scan-jandex-" + project.getVersion() + ".jar";
		final File hibernateScanJandexLibsFolder = getHibernateScanJandexBuildDirectory( project )
				.dir( "libs" )
				.getAsFile();
		return new File(hibernateScanJandexLibsFolder, artifactName).getAbsolutePath();
	}

	private Directory getHibernateCoreBuildDirectory(Project project) {
		return project
				.getRootProject()
				.project( "hibernate-core" )
				.getLayout()
				.getBuildDirectory()
				.get();
	}

	private Directory getHibernateScanJandexBuildDirectory(Project project) {
		return project
				.getRootProject()
				.project( "hibernate-scan-jandex" )
				.getLayout()
				.getBuildDirectory()
				.get();
	}

}
