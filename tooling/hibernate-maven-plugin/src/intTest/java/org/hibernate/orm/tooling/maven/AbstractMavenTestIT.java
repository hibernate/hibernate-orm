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
import java.util.Arrays;

public abstract class AbstractMavenTestIT {

	protected static final String MVN_HOME = "maven.multiModuleProjectDirectory";

	private static ClassWorld classWorld;
	private static MavenCli mavenCli;

	@BeforeAll
	public static void initMavenCli() throws Exception {
		classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
		mavenCli = new MavenCli( classWorld );
	}

	@AfterAll
	public static void closeMavenCli() throws Exception {
		classWorld.close();
	}

	protected void runMaven(String workingDirectory, String... goals) {
		System.setProperty( MVN_HOME, workingDirectory );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		int result = mavenCli.doMain(
				goals,
				workingDirectory,
				new PrintStream( out ),
				new PrintStream( err ) );
		assertEquals( 0, result,
				"Maven invocation failed for goals: " + Arrays.asList( goals )
				+ "\n--- stdout ---\n" + out
				+ "\n--- stderr ---\n" + err );
	}

}
