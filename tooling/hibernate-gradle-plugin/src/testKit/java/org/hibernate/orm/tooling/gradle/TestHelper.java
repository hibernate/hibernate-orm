/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
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
	public static Path testProjectsBaseDirectory() {
		final URL baseUrl = TestHelper.class.getResource( "/testkit_locator.properties" );
		if ( baseUrl == null ) {
			throw new IllegalStateException( "Could not locate marker" );
		}

		return new File( baseUrl.getFile() ).getParentFile().toPath();
	}

	public static Path projectDirectory(String projectName) {
		return testProjectsBaseDirectory().resolve( projectName );
	}

	public static GradleRunner gradleRunner(Path tempDirPath, String projectName, String... args) {
		final Path projectDirPath = tempDirPath.resolve( "project" ).resolve( projectName );
		projectDirPath.toFile().mkdirs();

		final Path testKitDirPath = tempDirPath.resolve( "testkit" );
		testKitDirPath.toFile().mkdirs();

		final Path projectSourceDirPath = projectDirectory( projectName );
		DirectoryCopier.copy( projectSourceDirPath, projectDirPath );

		final GradleRunner gradleRunner = GradleRunner.create();
		final List<String> arguments = new ArrayList<>( Arrays.asList( args ) );
		arguments.add( "--stacktrace" );

		return gradleRunner
				.withPluginClasspath()
				.withProjectDir( projectDirPath.toFile() )
				.withTestKitDir( testKitDirPath.toFile() )
				.forwardOutput()
				.withDebug( true )
				.withArguments( arguments );
	}
}
