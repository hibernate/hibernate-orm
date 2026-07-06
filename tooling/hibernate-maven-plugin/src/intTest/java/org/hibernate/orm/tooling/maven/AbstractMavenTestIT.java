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
		String centralFallback = System.getenv( "MIRROR_MAVEN_CENTRAL_FALLBACK" );
		String snapshotsMirror = System.getenv( "MIRROR_MAVEN_CENTRAL_SNAPSHOTS_URL" );
		String snapshotsMirrorUsername = System.getenv( "MIRROR_MAVEN_CENTRAL_SNAPSHOTS_USERNAME" );
		String snapshotsFallback = System.getenv( "MIRROR_MAVEN_CENTRAL_SNAPSHOTS_FALLBACK" );
		mavenSettingsFile = Files.createTempFile( "maven-settings", ".xml" );
		StringBuilder settings = new StringBuilder( "<settings>\n" );
		boolean hasCentralMirror = centralMirror != null && !centralMirror.isEmpty();
		boolean hasSnapshotsMirror = snapshotsMirror != null && !snapshotsMirror.isEmpty();
		if ( hasCentralMirror || hasSnapshotsMirror ) {
			settings.append( "<mirrors>\n" );
			if ( hasCentralMirror ) {
				settings.append( """
							<mirror>
							<id>ci-mirror-central</id>
							<mirrorOf>central</mirrorOf>
							<url>${env.MIRROR_MAVEN_CENTRAL_URL}</url>
							</mirror>
						""" );
			}
			if ( hasSnapshotsMirror ) {
				settings.append( """
							<mirror>
							<id>ci-mirror-snapshots</id>
							<mirrorOf>jakarta-snapshots</mirrorOf>
							<url>${env.MIRROR_MAVEN_CENTRAL_SNAPSHOTS_URL}</url>
							</mirror>
						""" );
			}
			settings.append( "</mirrors>\n" );
		}
		boolean hasServers = centralMirrorUsername != null || snapshotsMirrorUsername != null;
		if ( hasServers ) {
			settings.append( "<servers>\n" );
			if ( centralMirrorUsername != null ) {
				settings.append( """
							<server>
							<id>ci-mirror-central</id>
							<username>${env.MIRROR_MAVEN_CENTRAL_USERNAME}</username>
							<password>${env.MIRROR_MAVEN_CENTRAL_PASSWORD}</password>
							</server>
						""" );
			}
			if ( snapshotsMirrorUsername != null ) {
				settings.append( """
							<server>
							<id>ci-mirror-snapshots</id>
							<username>${env.MIRROR_MAVEN_CENTRAL_SNAPSHOTS_USERNAME}</username>
							<password>${env.MIRROR_MAVEN_CENTRAL_SNAPSHOTS_PASSWORD}</password>
							</server>
						""" );
			}
			settings.append( "</servers>\n" );
		}
		boolean hasCentralFallback = hasCentralMirror && "true".equalsIgnoreCase( centralFallback );
		boolean hasSnapshotsFallback = hasSnapshotsMirror && "true".equalsIgnoreCase( snapshotsFallback );
		settings.append( "<profiles>\n" );
		settings.append( """
					<profile>
					<id>jakarta-snapshots</id>
					<repositories>
						<repository>
						<id>jakarta-snapshots</id>
						<url>https://jakarta.oss.sonatype.org/content/repositories/snapshots/</url>
						<snapshots>
							<enabled>true</enabled>
						</snapshots>
						<releases>
							<enabled>false</enabled>
						</releases>
						</repository>
					</repositories>
					</profile>
				""" );
		if ( hasCentralFallback || hasSnapshotsFallback ) {
			settings.append( "<profile>\n<id>mirror-fallbacks</id>\n<repositories>\n" );
			if ( hasCentralFallback ) {
				settings.append( """
							<repository>
							<id>central-fallback</id>
							<url>https://repo.maven.apache.org/maven2/</url>
							<releases><enabled>true</enabled></releases>
							<snapshots><enabled>false</enabled></snapshots>
							</repository>
						""" );
			}
			if ( hasSnapshotsFallback ) {
				settings.append( """
							<repository>
							<id>jakarta-snapshots-fallback</id>
							<url>https://jakarta.oss.sonatype.org/content/repositories/snapshots/</url>
							<releases><enabled>false</enabled></releases>
							<snapshots><enabled>true</enabled></snapshots>
							</repository>
						""" );
			}
			settings.append( "</repositories>\n</profile>\n" );
		}
		settings.append( "</profiles>\n" );
		settings.append( "<activeProfiles>\n" );
		settings.append( "<activeProfile>jakarta-snapshots</activeProfile>\n" );
		if ( hasCentralFallback || hasSnapshotsFallback ) {
			settings.append( "<activeProfile>mirror-fallbacks</activeProfile>\n" );
		}
		settings.append( "</activeProfiles>\n" );
		settings.append( "</settings>\n" );
		Files.writeString( mavenSettingsFile, settings.toString() );
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
