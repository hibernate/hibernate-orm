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
 * @author Steve Ebersole
 */
public abstract class TestsBase {
	protected abstract String getProjectName();
	protected abstract String getSourceSetName();
	protected abstract String getLanguageName();
	protected abstract String getCompileTaskName();

	public void testEnhancement(Path projectDir) throws Exception {
		final String buildFilePath = getProjectName() + "/build.gradle";
		final String sourceSetName = getSourceSetName();
		final String compileTaskName = getCompileTaskName();

		final File classesDir = new File( projectDir.toFile(), "build/classes/" + getLanguageName() + "/" + sourceSetName );

		Copier.copyProject( buildFilePath, projectDir );

		System.out.println( "Starting execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		final GradleRunner gradleRunner = GradleRunner.create()
				.withProjectDir( projectDir.toFile() )
				.withPluginClasspath()
				.withDebug( true )
				.withArguments( "clean", compileTaskName, "--stacktrace", "--no-build-cache" )
				.forwardOutput();

		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":" + compileTaskName );
		assertThat( task ).isNotNull();
		assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		// make sure the class is enhanced
		final ClassLoader classLoader = Helper.toClassLoader( classesDir );
		TestHelper.verifyEnhanced( classLoader, "TheEmbeddable" );
		TestHelper.verifyEnhanced( classLoader, "TheEntity" );
	}

	@Test
	public void testEnhancementUpToDate(Path projectDir) throws Exception {
		final String buildFilePath = getProjectName() + "/build.gradle";
		final String sourceSetName = getSourceSetName();
		final String compileTaskName = getCompileTaskName();

		final File classesDir = new File( projectDir.toFile(), "build/classes/" + getLanguageName() + "/" + sourceSetName );

		Copier.copyProject( buildFilePath, projectDir );

		{
			System.out.println( "Starting first execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( "clean", compileTaskName, "--stacktrace", "--no-build-cache" )
					.forwardOutput();

			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":" + compileTaskName );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

			// make sure the class is enhanced
			final ClassLoader classLoader = Helper.toClassLoader( classesDir );
			TestHelper.verifyEnhanced( classLoader, "TheEntity" );
		}

		{
			System.out.println( "Starting second execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir( projectDir.toFile() )
					.withPluginClasspath()
					.withDebug( true )
					.withArguments( compileTaskName, "--stacktrace", "--no-build-cache" )
					.forwardOutput();
			final BuildResult result = gradleRunner.build();
			final BuildTask task = result.task( ":" + compileTaskName );
			assertThat( task ).isNotNull();
			assertThat( task.getOutcome() ).isEqualTo( TaskOutcome.UP_TO_DATE );

			// and again
			final ClassLoader classLoader = Helper.toClassLoader( classesDir );
			TestHelper.verifyEnhanced( classLoader, "TheEntity" );
		}
	}

	@Test
	public void testJpaMetamodelGen(@TempDir Path projectDir) {
		final String buildFilePath = getProjectName() + "/build.gradle";
		Copier.copyProject( buildFilePath, projectDir );

		System.out.println( "Starting execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

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
		final String buildFilePath = getProjectName() + "/build.gradle";
		Copier.copyProject( buildFilePath, projectDir );

		{
			System.out.println( "Starting first execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
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
			System.out.println( "Starting second execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
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
