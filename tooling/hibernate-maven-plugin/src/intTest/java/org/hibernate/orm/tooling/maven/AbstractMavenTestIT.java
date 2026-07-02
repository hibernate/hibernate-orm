/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public abstract class AbstractMavenTestIT {

	protected static final String MVN_HOME = "maven.multiModuleProjectDirectory";

	private static ClassWorld classWorld;
	private static MavenCli mavenCli;
	private static Path mavenSettingsFile;

	@BeforeAll
	public static void initMavenCli() throws Exception {
		classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
		mavenCli = new MavenCli( classWorld );
		String centralMirror = System.getenv( "MIRROR_MAVEN_CENTRAL_URL" );
		String centralMirrorUsername = System.getenv( "MIRROR_MAVEN_CENTRAL_USERNAME" );
		if ( centralMirror != null && !centralMirror.isEmpty() ) {
			mavenSettingsFile = Files.createTempFile( "maven-settings", ".xml" );
			StringBuilder settings = new StringBuilder( "<settings>\n" );
			settings.append( "  <mirrors>\n" );
			settings.append( "    <mirror>\n" );
			settings.append( "      <id>ci-mirror-central</id>\n" );
			settings.append( "      <mirrorOf>central</mirrorOf>\n" );
			settings.append( "      <url>${env.MIRROR_MAVEN_CENTRAL_URL}</url>\n" );
			settings.append( "    </mirror>\n" );
			settings.append( "  </mirrors>\n" );
			if ( centralMirrorUsername != null ) {
				settings.append( "  <servers>\n" );
				settings.append( "    <server>\n" );
				settings.append( "      <id>ci-mirror-central</id>\n" );
				settings.append( "      <username>${env.MIRROR_MAVEN_CENTRAL_USERNAME}</username>\n" );
				settings.append( "      <password>${env.MIRROR_MAVEN_CENTRAL_PASSWORD}</password>\n" );
				settings.append( "    </server>\n" );
				settings.append( "  </servers>\n" );
			}
			settings.append( "</settings>\n" );
			Files.writeString( mavenSettingsFile, settings.toString() );
		}
	}

	@AfterAll
	public static void closeMavenCli() throws Exception {
		classWorld.close();
		if ( mavenSettingsFile != null ) {
			Files.deleteIfExists( mavenSettingsFile );
			mavenSettingsFile = null;
		}
	}

	protected void runMaven(String workingDirectory, String... goals) {
		System.setProperty( MVN_HOME, workingDirectory );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		String[] args;
		if ( mavenSettingsFile != null ) {
			args = new String[goals.length + 2];
			args[0] = "-s";
			args[1] = mavenSettingsFile.toAbsolutePath().toString();
			System.arraycopy( goals, 0, args, 2, goals.length );
		}
		else {
			args = goals;
		}
		int result = mavenCli.doMain(
				args,
				workingDirectory,
				new PrintStream( out ),
				new PrintStream( err ) );
		assertEquals( 0, result,
				"Maven invocation failed for goals: " + Arrays.asList( goals )
				+ "\n--- stdout ---\n" + out
				+ "\n--- stderr ---\n" + err );
	}

}
