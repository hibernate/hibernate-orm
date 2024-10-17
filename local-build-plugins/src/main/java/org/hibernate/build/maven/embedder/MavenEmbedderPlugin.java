package org.hibernate.build.maven.embedder;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

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

		// add the MavenEmbedderService shared-build-service
		final Provider<MavenEmbedderService> embedderServiceProvider = sharedServices.registerIfAbsent(
				"maven-embedder",
				MavenEmbedderService.class, (spec) -> {
					spec.getParameters().getProjectVersion().set( project.getVersion().toString() );
					spec.getParameters().getWorkingDirectory().set( project.getLayout().getProjectDirectory() );
					spec.getParameters().getMavenLocalDirectory().set( dsl.getLocalRepositoryDirectory() );
				}
		);

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

			// we need compilation to happen before we generate the descriptors
			task.dependsOn( "compileJava" );
		} );

		// we need the descriptor generation to happen before we jar
		project.getTasks().named( "jar", (jarTask) -> jarTask.dependsOn( generatePluginDescriptorTask ) );
	}
}
