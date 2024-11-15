package org.hibernate.build.maven.embedder;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

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

		final Provider<RegularFile> mavenPluginPom = project.getLayout().getBuildDirectory().file( "publications/publishedArtifacts/pom-default.xml" );

		final Project hibernateCoreProject = project.getRootProject().project( "hibernate-core" );
		final DirectoryProperty hibernateCoreBuildDirectory = hibernateCoreProject.getLayout().getBuildDirectory();
		final Provider<Directory> hibernateCoreLibsFolder  = hibernateCoreBuildDirectory.dir("libs");
		final Provider<RegularFile> hibernateCorePom = hibernateCoreBuildDirectory.file( "publications/publishedArtifacts/pom-default.xml" );
		final TaskProvider<MavenInstallArtifactTask> installHibernateCoreTask = project.getTasks().register( "installHibernateCore", MavenInstallArtifactTask.class, (task) -> {
			task.setGroup( "maven embedder" );
			task.getMavenEmbedderService().set( embedderServiceProvider );
			task.usesService( embedderServiceProvider );
			final String artifactName = "hibernate-core-" + project.getVersion() + ".jar";
			task.artifactId = "hibernate-core";
			task.getArtifact().set( new File( hibernateCoreLibsFolder.get().getAsFile(), artifactName ));
			task.pomFilePath = hibernateCorePom.get().getAsFile().getAbsolutePath();
			task.dependsOn(":hibernate-core:generatePomFileForPublishedArtifactsPublication", ":hibernate-core:jar");
		} );

		final Project scanJandexProject = project.getRootProject().project( "hibernate-scan-jandex" );
		final Provider<Directory> hibernateScanJandexLibsFolder  = scanJandexProject.getLayout().getBuildDirectory().dir("libs");
		final TaskProvider<MavenInstallArtifactTask> installHibernateScanJandexTask = project.getTasks().register( "installHibernateScanJandex", MavenInstallArtifactTask.class, (task) -> {
			task.setGroup( "maven embedder" );
			task.getMavenEmbedderService().set( embedderServiceProvider );
			task.usesService( embedderServiceProvider );
			final String artifactName = "hibernate-scan-jandex-" + project.getVersion() + ".jar";
			task.artifactId = "hibernate-scan-jandex";
			task.getArtifact().set( new File(hibernateScanJandexLibsFolder.get().getAsFile(), artifactName ));
			task.dependsOn( ":hibernate-scan-jandex:jar" );
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

		final TaskProvider<MavenWrapperTask> createMavenWrapperTask = project.getTasks().register( "createMavenWrapper", MavenWrapperTask.class, (task) -> {
			task.setGroup("maven embedder");
			task.getMavenEmbedderService().set( embedderServiceProvider );
			task.usesService( embedderServiceProvider );
			task.getIntegrationTestSourcesFolder().set( workingDirectory.get().dir( "src/it/enhance" ) );
			task.dependsOn( "prepareWorkspace" );
		} );

		final TaskProvider<MavenInstallArtifactTask> installHibernateMavenPluginTask = project.getTasks().register( "installHibernateMavenPlugin", MavenInstallArtifactTask.class, (task) -> {
			task.setGroup( "maven embedder" );
			task.getMavenEmbedderService().set( embedderServiceProvider );
			task.usesService( embedderServiceProvider );
			final String artifactName = "hibernate-maven-plugin-" + project.getVersion() + ".jar";
			task.artifactId = "hibernate-maven-plugin";
			task.getArtifact().set( new File(project.getLayout().getBuildDirectory().dir("libs" ).get().getAsFile(), artifactName));
			task.pomFilePath = mavenPluginPom.get().getAsFile().getAbsolutePath();
			task.dependsOn("jar", generatePluginDescriptorTask);
		} );

		final TaskProvider<MavenInvokerRunTask> integrationTestTask = project.getTasks().register( "integrationTest", MavenInvokerRunTask.class, (task) -> {
			task.setGroup( "maven embedder" );

			task.getMavenEmbedderService().set( embedderServiceProvider );
			task.usesService( embedderServiceProvider );

			task.dependsOn("createMavenWrapper", "installHibernateMavenPlugin");

		} );

		// we need the descriptor generation to happen before we jar
		project.getTasks().named( "jar", (jarTask) -> jarTask.dependsOn( generatePluginDescriptorTask ) );
		project.getTasks().named( "check" , (checkTask) -> checkTask.dependsOn( integrationTestTask, generatePluginDescriptorTask ) );
	}
}
