/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.testkit.runner.GradleRunner;

/**
 * @author Steve Ebersole
 */
public class TestHelper {

	// our output is the base for all of the test projects, so we must be
	// able to locate that..  this is our `${buildDir}/resources/test` directory
	// (or IDE equivalent) within that directory we will have access to the Maven
	// `settings.xml` as well as the various test project root dirs
	public static File testProjectsBaseDirectory() {
		final URL baseUrl = TestHelper.class.getResource( "/projects/test_project_out_dir_locator.properties" );
		return new File( baseUrl.getFile() ).getParentFile();
	}

	public static File projectDirectory(String projectName) {
		return new File( testProjectsBaseDirectory(), projectName );
	}

	public static GradleRunner gradleRunner(String projectName, String... args) {
		final File projectDirectory = projectDirectory( projectName );
		final File testKitDir = new File(
				new File(
						// this should be the "real" `${buildDir}` directory
						projectDirectory.getParentFile().getParentFile().getParentFile(),
						"tmp"
				),
				"test-kit"
		);

		final GradleRunner gradleRunner = GradleRunner.create();
		final List<String> arguments = new ArrayList<>( Arrays.asList( args ) );
		arguments.add( "--stacktrace" );

		return gradleRunner
				.withPluginClasspath()
				.withProjectDir( projectDirectory )
				.withTestKitDir( testKitDir )
				.forwardOutput()
				.withDebug( true )
				.withArguments( arguments );
	}
}
