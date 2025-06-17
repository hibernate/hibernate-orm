/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.toolchains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;

/**
 * Retrieves JDK versions exposed by {@link JdkVersionPlugin}
 * and injects them in build tasks.
 *
 * @see JdkVersionConfig
 * @see JdkVersionSettingsPlugin
 * @see JdkVersionPlugin
 *
 * @author Steve Ebersole
 */
public class JavaModulePlugin implements Plugin<Project> {
	private final JavaToolchainService toolchainService;

	@Inject
	public JavaModulePlugin(JavaToolchainService toolchainService) {
		this.toolchainService = toolchainService;
	}

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply( JdkVersionPlugin.class );

		final JdkVersionConfig jdkVersionsConfig = project.getExtensions().getByType( JdkVersionConfig.class );

		final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType( JavaPluginExtension.class );

		final SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
		final SourceSet mainSourceSet = sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME );
		final SourceSet testSourceSet = sourceSets.getByName( SourceSet.TEST_SOURCE_SET_NAME );

		final JavaCompile mainCompileTask = (JavaCompile) project.getTasks().getByName( mainSourceSet.getCompileJavaTaskName() );
		final JavaCompile testCompileTask = (JavaCompile) project.getTasks().getByName( testSourceSet.getCompileJavaTaskName() );
		final Test testTask = (Test) project.getTasks().findByName( testSourceSet.getName() );

		if ( !jdkVersionsConfig.isExplicitlyConfigured() ) {
			mainCompileTask.setSourceCompatibility( jdkVersionsConfig.getMainReleaseVersion().toString() );
			mainCompileTask.setTargetCompatibility( jdkVersionsConfig.getMainReleaseVersion().toString() );

			testCompileTask.setSourceCompatibility( jdkVersionsConfig.getTestReleaseVersion().toString() );
			testCompileTask.setTargetCompatibility( jdkVersionsConfig.getTestReleaseVersion().toString() );
		}
		else {
			javaPluginExtension.getToolchain().getLanguageVersion().set( jdkVersionsConfig.getMainCompilerVersion() );

			configureCompileTasks( project );
			configureTestTasks( project );
			configureJavadocTasks( project, mainSourceSet );

			configureCompileTask( mainCompileTask, jdkVersionsConfig.getMainReleaseVersion() );
			configureCompileTask( testCompileTask, jdkVersionsConfig.getTestReleaseVersion() );

			testCompileTask.getJavaCompiler().set(
					toolchainService.compilerFor( javaToolchainSpec -> {
						javaToolchainSpec.getLanguageVersion().set( jdkVersionsConfig.getTestCompilerVersion() );
					} )
			);
			if ( testTask != null ) {
				testTask.getJavaLauncher().set(
						toolchainService.launcherFor( javaToolchainSpec -> {
							javaToolchainSpec.getLanguageVersion().set( jdkVersionsConfig.getTestLauncherVersion() );
						} )
				);
			}
		}
	}

	private void configureCompileTask(JavaCompile compileTask, JavaLanguageVersion releaseVersion) {
		final CompileOptions compileTaskOptions = compileTask.getOptions();
		compileTaskOptions.getRelease().set( releaseVersion.asInt() );
	}

	private void configureCompileTasks(Project project) {
		project.getTasks().withType( JavaCompile.class ).configureEach( new Action<JavaCompile>() {
			@Override
			public void execute(JavaCompile compileTask) {
				addJvmArgs( compileTask,
						project.property( "toolchain.compiler.jvmargs" ).toString().split( " " )
				);
				compileTask.doFirst(
						new Action<Task>() {
							@Override
							public void execute(Task task) {
								project.getLogger().lifecycle(
										"Compiling with '{}'",
										compileTask.getJavaCompiler().get().getMetadata().getInstallationPath()
								);
							}
						}
				);
			}
		} );
	}

	private void configureTestTasks(Project project) {
		project.getTasks().withType( Test.class ).configureEach( new Action<Test>() {
			@Override
			public void execute(Test testTask) {
				testTask.jvmArgs(
						Arrays.asList(
								project.property( "toolchain.launcher.jvmargs" ).toString().split( " " )
						)
				);
				if ( project.hasProperty( "test.jdk.launcher.args" ) ) {
					testTask.jvmArgs(
							Arrays.asList(
								project.getProperties().get( "test.jdk.launcher.args" ).toString().split( " " )
							)
					);
				}
				testTask.doFirst(
						new Action<Task>() {
							@Override
							public void execute(Task task) {
								project.getLogger().lifecycle(
										"Testing with '{}'",
										testTask.getJavaLauncher().get().getMetadata().getInstallationPath()
								);
							}
						}
				);
			}
		} );
	}

	private void configureJavadocTasks(Project project, SourceSet mainSourceSet) {
		project.getTasks().named( mainSourceSet.getJavadocTaskName(), Javadoc.class, (task) -> {
			task.getOptions().setJFlags( javadocFlags( project ) );
			task.doFirst( new Action<Task>() {
				@Override
				public void execute(Task t) {
					project.getLogger().lifecycle(
							"Generating javadoc with '{}'",
							task.getJavadocTool().get().getMetadata().getInstallationPath()
					);
				}
			} );
		} );
	}

	private static List<String> javadocFlags(Project project) {
		final String jvmArgs = project.property( "toolchain.javadoc.jvmargs" ).toString();
		final String[] splits = jvmArgs.split( " " );
		return Arrays.asList( splits ).stream().filter( (split) -> !split.isEmpty() ).collect( Collectors.toList() );
	}

	public static void addJvmArgs(JavaCompile compileTask, String ... newArgs) {
		ForkOptions forOptions = compileTask
				.getOptions()
				.getForkOptions();
		final List<String> mergedArgs = new ArrayList<>( forOptions.getJvmArgs() );
		Collections.addAll( mergedArgs, newArgs );
		forOptions.setJvmArgs( mergedArgs );
	}
}
