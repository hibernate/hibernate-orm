/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.hibernate.tool.reveng.api.version.Version;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformHbmTestIT {

	public static final String MVN_HOME = "maven.multiModuleProjectDirectory";

	private static ClassWorld classWorld;
	private static MavenCli mavenCli;

	@TempDir
	private Path projectPath;

	@BeforeAll
	public static void beforeAll() throws Exception {
		classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
		mavenCli = new MavenCli( classWorld );
	}

	@AfterAll
	public static void afterAll() throws Exception {
		classWorld.close();
	}

	@Test
	public void testSimpleHbmTransformation() throws Exception {
		System.setProperty(MVN_HOME, projectPath.toAbsolutePath().toString());
		writePomFile();
		copyHbmFile();
		runTransformHbmToOrm();
	}

	private void writePomFile() throws Exception {
		File pomFile = new File(projectPath.toFile(), "pom.xml");
		assertFalse(pomFile.exists());
		Path pomPath = projectPath.resolve("pom.xml");
		Files.writeString(pomPath, simplePomContents);
		assertTrue(pomFile.exists());
	}

	private void copyHbmFile() throws Exception {
		URL originUrl = getClass().getResource("simple.hbm.xml");
		assertNotNull(originUrl);
		Path originPath = Paths.get(Objects.requireNonNull(originUrl).toURI());
		File destinationDir = new File(projectPath.toFile(), "src/main/resources/");
		assertTrue(destinationDir.mkdirs());
		File destinationFile = new File(destinationDir, "simple.hbm.xml");
		assertFalse(destinationFile.exists());
		Files.copy(originPath, destinationFile.toPath());
		assertTrue(destinationFile.exists());
	}

	private void runTransformHbmToOrm() throws Exception {
		File destinationDir = new File(projectPath.toFile(), "src/main/resources/");
		File ormXmlFile = new File(destinationDir, "simple.mapping.xml");
		assertFalse(ormXmlFile.exists());
		runMaven(
				"compile",
				"org.hibernate.orm:hibernate-maven-plugin:" + Version.versionString() + ":transformHbm" );
		// Check the existence of the transformed file
		assertTrue(ormXmlFile.exists());
		// Check if it's pretty printed
		assertTrue(Files.readString(ormXmlFile.toPath()).contains("\n        <table name=\"Foo\"/>\n"));
	}

	private void runMaven(String... goals) {
		int result = mavenCli.doMain(
				goals,
				projectPath.toAbsolutePath().toString(),
				System.out,
				System.err );
		assertEquals( 0, result, "Maven invocation failed for goals: " + Arrays.asList( goals ) );
	}

	private static final String simplePomContents =
			"""
				<project>
					<modelVersion>4.0.0</modelVersion>
					<groupId>org.hibernate.tool.maven.test</groupId>
					<artifactId>simplest</artifactId>
					<version>0.1-SNAPSHOT</version>
				</project>
				""";

}
