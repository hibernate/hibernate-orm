/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.hibernate.orm.tooling.gradle.TestHelper.usingGradleRunner;


/**
 * Basic functional tests
 *
 * @author Steve Ebersole
 */
class HbmTransformerTests {
	@Test
	void testSimpleTransformation(@TempDir Path projectDir) throws IOException {
		final String buildFilePath = "hbm/build.gradle";

		Copier.copyProject( buildFilePath, projectDir );

		System.out.println( "Starting execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		final GradleRunner gradleRunner = usingGradleRunner()
				.withProjectDir( projectDir.toFile() )
				.withArguments( "clean", "hbmTransform", "--stacktrace", "--no-build-cache", "--configuration-cache" );
		final BuildResult result = gradleRunner.build();

		final BuildTask transformationResult = result.task( ":hbmTransform" );
		assertThat( transformationResult ).isNotNull();
		assertThat( transformationResult.getOutcome() ).isEqualTo( SUCCESS );

		final File targetDir = new File( projectDir.toFile(), "build" );
		final File transformationOutputDir = new File( targetDir, "resources/hbm-transformed" );
		assertThat( transformationOutputDir ).exists();
		assertThat( transformationOutputDir ).isNotEmptyDirectory();

		final File[] transformedFiles = transformationOutputDir.listFiles();
		assertThat( transformedFiles ).hasSize( 1 );
		final String transformedFileContent = Files.readString( transformedFiles[0].toPath() );
		assertThat( transformedFileContent ).doesNotContain( "<hibernate-mapping" );
		assertThat( transformedFileContent ).doesNotContain( "</hibernate-mapping>" );
		assertThat( transformedFileContent ).contains( "<entity-mappings" );
		assertThat( transformedFileContent ).contains( "<entity name=\"simple\" metadata-complete=\"true\">" );
		assertThat( transformedFileContent ).contains( "</entity-mappings>" );
	}
}
