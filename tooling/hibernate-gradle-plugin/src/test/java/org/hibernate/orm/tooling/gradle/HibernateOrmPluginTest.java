/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.tooling.gradle;

import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.assertj.core.api.Condition;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


/**
 * Basic functional tests
 *
 * @author Steve Ebersole
 */
class HibernateOrmPluginTest {
	private static final Condition<TaskOutcome> SUCCESS = new Condition<>(
			(taskOutcome) -> taskOutcome == TaskOutcome.SUCCESS,
			"task succeeded"
	);
	private static final Condition<TaskOutcome> UP_TO_DATE = new Condition<>(
			(taskOutcome) -> taskOutcome == TaskOutcome.UP_TO_DATE,
			"task up-to-date"
	);

	@Test
	public void testEnhancementTask(@TempDir Path projectDir) {
		Copier.copyProject( "simple/build.gradle", projectDir );

		System.out.println( "First execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		final GradleRunner gradleRunner = GradleRunner.create()
				.withProjectDir( projectDir.toFile() )
				.withPluginClasspath()
				.withDebug( true )
				.withArguments( "clean", "hibernateEnhance", "--stacktrace", "--no-build-cache" )
				.forwardOutput();

		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":hibernateEnhance" );
		assertThat( task ).isNotNull();
		assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void testEnhancementTaskAsFinalizer(@TempDir Path projectDir) {
		Copier.copyProject( "simple/build.gradle", projectDir );

		final GradleRunner gradleRunner = GradleRunner.create()
				.withProjectDir( projectDir.toFile() )
				.withPluginClasspath()
				.withDebug( true )
				.withArguments( "clean", "compileJava", "--stacktrace", "--no-build-cache" )
				.forwardOutput();

		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":hibernateEnhance" );
		assertThat( task ).isNotNull();
//		assertThat( task.getOutcome() ).is( anyOf( SUCCESS, UP_TO_DATE ) );
		assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	@Disabled( "up-to-date checking not working" )
	public void testEnhancementTaskUpToDate(@TempDir Path projectDir) {
		Copier.copyProject( "simple/build.gradle", projectDir );

		{
			System.out.println( "First execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "clean", "hibernateEnhance", "--stacktrace", "--no-build-cache" )
					.forwardOutput();

			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":hibernateEnhance" );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
		}

		{
			System.out.println( "Second execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "hibernateEnhance", "--stacktrace", "--no-build-cache" )
					.forwardOutput();
			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":hibernateEnhance" );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.UP_TO_DATE );
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
	}

	@Test
	@Disabled( "up-to-date checking not working" )
	public void testJpaMetamodelGenUpToDate(@TempDir Path projectDir) {
		Copier.copyProject( "simple/build.gradle", projectDir );

		{
			System.out.println( "First execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "clean", "generateJpaMetamodel", "-xhibernateEnhance", "--stacktrace", "--no-build-cache" )
					.forwardOutput();

			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":generateJpaMetamodel" );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
		}

		{
			System.out.println( "Second execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner2 = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "clean", "generateJpaMetamodel", "-xhibernateEnhance", "--stacktrace", "--no-build-cache" )
					.forwardOutput();

			final BuildResult result2 = gradleRunner2.build();
			final BuildTask task2 = result2.task( ":generateJpaMetamodel" );
			assertThat( task2 ).isNotNull();
			assertThat( task2.getOutcome() ).isEqualTo( TaskOutcome.UP_TO_DATE );
		}
	}

	@Test
	@Disabled( "HHH-15314" )
	public void testEnhanceKotlinModel(@TempDir Path projectDir) {
		Copier.copyProject( "simple-kotlin/build.gradle", projectDir );

		final GradleRunner gradleRunner = GradleRunner.create()
				.withProjectDir( projectDir.toFile() )
				.withPluginClasspath()
				.withDebug( true )
				.withArguments( "clean", "hibernateEnhance", "--stacktrace", "--no-build-cache" )
				.forwardOutput();

		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":hibernateEnhance" );
		assertThat( task ).isNotNull();
	}
}
