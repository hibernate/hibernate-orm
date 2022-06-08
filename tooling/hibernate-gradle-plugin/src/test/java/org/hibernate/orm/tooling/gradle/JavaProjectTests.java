/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


/**
 * Basic functional tests
 *
 * @author Steve Ebersole
 */
class JavaProjectTests {

	@Test
	public void testEnhancement(@TempDir Path projectDir) throws Exception {
		Copier.copyProject( "simple/build.gradle", projectDir );

		System.out.println( "First execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		final GradleRunner gradleRunner = GradleRunner.create()
				.withProjectDir( projectDir.toFile() )
				.withPluginClasspath()
				.withDebug( true )
				.withArguments( "clean", "compileJava", "--stacktrace", "--no-build-cache" )
				.forwardOutput();

		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":compileJava" );
		assertThat( task ).isNotNull();
		assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		// make sure the class is enhanced
		final File classesDir = new File( projectDir.toFile(), "build/classes/java/main" );
		final ClassLoader classLoader = Helper.toClassLoader( classesDir );
		TestHelper.verifyEnhanced( classLoader, "TheEmbeddable" );
		TestHelper.verifyEnhanced( classLoader, "TheEntity" );
	}

	@Test
	public void testEnhancementUpToDate(@TempDir Path projectDir) throws Exception {
		Copier.copyProject( "simple/build.gradle", projectDir );

		{
			System.out.println( "First execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "clean", "compileJava", "--stacktrace", "--no-build-cache" )
					.forwardOutput();

			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":compileJava" );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

			// make sure the class is enhanced
			final File classesDir = new File( projectDir.toFile(), "build/classes/java/main" );
			final ClassLoader classLoader = Helper.toClassLoader( classesDir );
			TestHelper.verifyEnhanced( classLoader, "TheEntity" );
		}

		{
			System.out.println( "Second execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "compileJava", "--stacktrace", "--no-build-cache" )
					.forwardOutput();
			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":compileJava" );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.UP_TO_DATE );

			// and again
			final File classesDir = new File( projectDir.toFile(), "build/classes/java/main" );
			final ClassLoader classLoader = Helper.toClassLoader( classesDir );
			TestHelper.verifyEnhanced( classLoader, "TheEntity" );
		}
	}

	@Test
	public void testJpaMetamodelGen(@TempDir Path projectDir) {
		Copier.copyProject( "simple/build.gradle", projectDir );

		final GradleRunner gradleRunner = GradleRunner.create()
				.withProjectDir( projectDir.toFile() )
				.withPluginClasspath()
				.withDebug( true )
				.withArguments( "clean", "generateJpaMetamodel", "--stacktrace", "--no-build-cache" )
				.forwardOutput();

		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":generateJpaMetamodel" );
		assertThat( task ).isNotNull();
		assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
		assertThat( new File( projectDir.toFile(), "build/classes/java/jpaMetamodel" ) ).exists();
	}

	@Test
	public void testJpaMetamodelGenUpToDate(@TempDir Path projectDir) {
		Copier.copyProject( "simple/build.gradle", projectDir );

		{
			System.out.println( "First execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "clean", "generateJpaMetamodel", "--stacktrace", "--no-build-cache" )
					.forwardOutput();

			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":generateJpaMetamodel" );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
			assertThat( new File( projectDir.toFile(), "build/classes/java/jpaMetamodel" ) ).exists();
		}

		{
			System.out.println( "Second execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner2 = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "generateJpaMetamodel", "--stacktrace", "--no-build-cache" )
					.forwardOutput();

			final BuildResult result2 = gradleRunner2.build();
			final BuildTask task2 = result2.task( ":generateJpaMetamodel" );
			assertThat( task2 ).isNotNull();
			assertThat( task2.getOutcome() ).isEqualTo( TaskOutcome.UP_TO_DATE );
			assertThat( new File( projectDir.toFile(), "build/classes/java/jpaMetamodel" ) ).exists();
		}
	}
}
