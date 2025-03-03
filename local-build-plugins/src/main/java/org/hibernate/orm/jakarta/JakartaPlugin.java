/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.jvm.tasks.ProcessResources;

import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;

/**
 * @author Steve Ebersole
 */
public class JakartaPlugin implements Plugin<Project> {
	public static final String JAKARTA = "jakarta";

	@Override
	public void apply(Project project) {
		// register short-names for the task classes (fake "import")
		project.getExtensions().getExtraProperties().set( JakartaDirectoryTransformation.class.getSimpleName(), JakartaDirectoryTransformation.class );
		project.getExtensions().getExtraProperties().set( JakartaJarTransformation.class.getSimpleName(), JakartaJarTransformation.class );

		final Configuration api = project.getConfigurations().create(
				"api",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( false );
				}
		);

		final Configuration implementation = project.getConfigurations().create(
				"implementation",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( false );
					configuration.extendsFrom( api );
				}
		);

		final Configuration compileOnly = project.getConfigurations().create(
				"compileOnly",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( false );
				}
		);

		final Configuration runtimeOnly = project.getConfigurations().create(
				"runtimeOnly",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( false );
				}
		);

		project.getConfigurations().create(
				"compileClasspath",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( true );
					configuration.extendsFrom( compileOnly, implementation );
				}
		);


		project.getConfigurations().create(
				"runtimeClasspath",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( true );
					configuration.extendsFrom( runtimeOnly, implementation );
				}
		);

		final Configuration testImplementation = project.getConfigurations().create(
				"testImplementation",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( false );
					configuration.extendsFrom( implementation );
				}
		);

		final Configuration testCompileOnly = project.getConfigurations().create(
				"testCompileOnly",
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( false );
					configuration.extendsFrom( compileOnly );
				}
		);

		final Configuration testRuntimeOnly = project.getConfigurations().create(
				"testRuntimeOnly" ,
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( false );
					configuration.extendsFrom( runtimeOnly );
				}
		);

		final Configuration testCompileClasspath = project.getConfigurations().create(
				"testCompileClasspath" ,
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( true );
					configuration.extendsFrom( testImplementation, testCompileOnly );
				}
		);

		final Configuration testRuntimeClasspath = project.getConfigurations().create(
				"testRuntimeClasspath" ,
				(configuration) -> {
					configuration.setCanBeConsumed( false );
					configuration.setCanBeResolved( true );
					configuration.extendsFrom( testImplementation, testRuntimeOnly );
				}
		);

		// determine the "source" project
		final String path = project.getPath();
		assert path.endsWith( "-jakarta" ) : "Project path did not end with `-jakarta`";
		final String sourceProjectPath = path.substring( 0, path.length() - 8 );
		final Project sourceProject = project.getRootProject().project( sourceProjectPath );


		// Get tasks from the source project we will need
		final TaskContainer sourceProjectTasks = sourceProject.getTasks();
		final SourceSetContainer sourceProjectSourceSets = extractSourceSets( sourceProject );
		final SourceSet sourceProjectMainSourceSet = sourceProjectSourceSets.getByName( MAIN_SOURCE_SET_NAME );
		final Jar sourceProjectJarTask = (Jar) sourceProjectTasks.getByName( sourceProjectMainSourceSet.getJarTaskName() );
		final Jar sourceProjectSourcesJarTask = (Jar) sourceProjectTasks.getByName( sourceProjectMainSourceSet.getSourcesJarTaskName() );
		final Jar sourceProjectJavadocJarTask = (Jar) sourceProjectTasks.getByName( sourceProjectMainSourceSet.getJavadocJarTaskName() );
		final SourceSet sourceProjectTestSourceSet = sourceProjectSourceSets.getByName( TEST_SOURCE_SET_NAME );
		final JavaCompile sourceProjectCompileTestClassesTask = (JavaCompile) sourceProjectTasks.getByName( sourceProjectTestSourceSet.getCompileJavaTaskName() );
		final ProcessResources sourceProjectProcessTestResourcesTask = (ProcessResources) sourceProjectTasks.getByName( sourceProjectTestSourceSet.getProcessResourcesTaskName() );


		// Create the "jakartafication" assemble tasks
		final TaskContainer tasks = project.getTasks();
		final Task jakartafyTask = tasks.create(
				"jakartafy",
				(task) -> {
					task.setDescription( "Performs all of the Jakarta transformations" );
					task.setGroup( JAKARTA );
				}
		);

		final DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();

		tasks.create(
				"jakartafyJar",
				JakartaJarTransformation.class,
				(transformation) -> {
					transformation.dependsOn( sourceProjectJarTask );
					transformation.setDescription( "Transforms the source project's main jar" );
					transformation.setGroup( JAKARTA );
					transformation.getSourceJar().convention( sourceProjectJarTask.getArchiveFile() );
					transformation.getTargetJar().convention( buildDirectory.file( relativeArchiveFileName( project, null ) ) );
					jakartafyTask.dependsOn( transformation );
				}
		);

		tasks.create(
				"jakartafySourcesJar",
				JakartaJarTransformation.class,
				(transformation) -> {
					transformation.dependsOn( sourceProjectSourcesJarTask );
					transformation.setDescription( "Transforms the source project's sources jar" );
					transformation.setGroup( JAKARTA );
					transformation.getSourceJar().convention( sourceProjectSourcesJarTask.getArchiveFile() );
					transformation.getTargetJar().convention( buildDirectory.file( relativeArchiveFileName( project, "sources" ) ) );
					jakartafyTask.dependsOn( transformation );
				}
		);

		tasks.create(
				"jakartafyJavadocJar",
				JakartaJarTransformation.class,
				(transformation) -> {
					transformation.dependsOn( sourceProjectJavadocJarTask );
					transformation.setDescription( "Transforms the source project's javadoc jar" );
					transformation.setGroup( JAKARTA );
					transformation.getSourceJar().convention( sourceProjectJavadocJarTask.getArchiveFile() );
					transformation.getTargetJar().convention( buildDirectory.file( relativeArchiveFileName( project, "javadoc" ) ) );
					jakartafyTask.dependsOn( transformation );
				}
		);

		final Provider<Directory> testCollectDir = project.getLayout().getBuildDirectory().dir( "jakarta/collect/tests" );
		final Provider<Directory> testTransformedDir = project.getLayout().getBuildDirectory().dir( "jakarta/transformed/tests" );

		final Copy collectTests = tasks.create(
				"collectTests",
				Copy.class,
				(task) -> {
					task.dependsOn( sourceProjectCompileTestClassesTask, sourceProjectProcessTestResourcesTask );
					task.setDescription( "Collects all needed test classes and resources into a single directory for transformation" );
					task.setGroup( JAKARTA );
					task.from( sourceProjectTestSourceSet.getOutput() );
					task.into( testCollectDir );
				}
		);

		final JakartaDirectoryTransformation jakartafyTests = tasks.create(
				"jakartafyTests",
				JakartaDirectoryTransformation.class,
				(task) -> {
					task.dependsOn( collectTests );
					task.setDescription( "Jakartafies the tests in preparation for execution" );
					task.setGroup( JAKARTA );
					task.getSourceDirectory().convention( testCollectDir );
					task.getTargetDirectory().convention( testTransformedDir );
				}
		);

		tasks.create(
				"test",
				Test.class,
				(task) -> {
					task.dependsOn( jakartafyTests );
					task.setDescription( "Performs the jakartafied tests against the jakartafied artifact" );
					task.setGroup( JAKARTA );

					final ConfigurableFileCollection transformedTests = project.files( testTransformedDir );
					task.setTestClassesDirs( transformedTests );
					task.setClasspath( task.getClasspath().plus( transformedTests ).plus( testRuntimeClasspath ) );

					project.getLayout().getBuildDirectory();
					task.getBinaryResultsDirectory().convention( project.getLayout().getBuildDirectory().dir( "test-results/test/binary" ) );
					task.reports( (reports) -> {
						reports.getHtml().getOutputLocation().convention( buildDirectory.dir( "reports/tests/test" ) );
						reports.getJunitXml().getOutputLocation().convention( buildDirectory.dir( "test-results/test" ) );
					});
				}
		);
	}

	public static String relativeArchiveFileName(Project project, String classifier) {
		final StringBuilder nameBuilder = new StringBuilder( "lib/" );
		nameBuilder.append( project.getName() );
		nameBuilder.append( "-" ).append( project.getVersion() );
		if ( classifier != null ) {
			nameBuilder.append( "-" ).append( classifier );
		}
		return nameBuilder.append( ".jar" ).toString();
	}

	public static SourceSetContainer extractSourceSets(Project project) {
		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		assert javaPluginConvention != null;
		return javaPluginConvention.getSourceSets();
	}
}
