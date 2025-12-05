/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.testkit.runner.GradleRunner;
import org.hibernate.engine.spi.Managed;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
public class TestHelper {
	public static final String MINIMUM_GRADLE_VERSION;

	static {
		// running from IDE breaks in a number of ways. one way to tell here
		// that we are running in the IDE is that this system property will
		// not be set.
		//
		// To be clear, the problem is not the version - we could hack that here.
		// The problem is actually that the --add-opens applied in the build
		// script do not get applied.
		MINIMUM_GRADLE_VERSION = System.getProperty( "orm.gradle.min" );
		if ( MINIMUM_GRADLE_VERSION == null ) {
			throw new IllegalStateException( "Gradle plugin tests cannot be run from IDE" );
		}
		System.out.printf( "Testing ORM Gradle plugin using Gradle version %s\n", MINIMUM_GRADLE_VERSION );
	}

	public static GradleRunner usingGradleRunner() {
		return GradleRunner.create()
				.withGradleVersion( MINIMUM_GRADLE_VERSION )
				.withPluginClasspath()
				.withDebug( true )
				.forwardOutput();
	}

	public static void verifyEnhanced(ClassLoader classLoader, String className) throws Exception {
		final Class<?> loadedClass = classLoader.loadClass( className );
		assertThat( Managed.class ).isAssignableFrom( loadedClass );
	}
}
