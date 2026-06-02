/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.env;

import org.gradle.api.artifacts.dsl.RepositoryHandler;

/**
 * Configures Maven Central or a mirror of it, based on the {@code MAVEN_MIRROR} environment
 * variable or system property. Optionally supports authentication via
 * {@code MAVEN_MIRROR_USERNAME} and {@code MAVEN_MIRROR_PASSWORD}.
 *
 * @see <a href="https://blog.gradle.org/maven-central-mirror">Gradle Maven Central Mirror</a>
 */
public class MavenMirror {

	/**
	 * @return {@code true} if a mirror was configured, {@code false} if plain Maven Central was added
	 */
	public static boolean maybeAddMavenCentral(RepositoryHandler repositories) {
		String mirror = resolve( "MAVEN_MIRROR" );
		if ( mirror != null ) {
			repositories.maven( repo -> {
				repo.setUrl( mirror );
				String username = resolve( "MAVEN_MIRROR_USERNAME" );
				if ( username != null ) {
					repo.credentials( creds -> {
						creds.setUsername( username );
						String password = resolve( "MAVEN_MIRROR_PASSWORD" );
						if ( password != null ) {
							creds.setPassword( password );
						}
					} );
				}
			} );
			return true;
		}
		else {
			repositories.mavenCentral();
			return false;
		}
	}

	private static String resolve(String key) {
		String value = System.getProperty( key );
		if ( value != null && !value.isEmpty() ) {
			return value;
		}
		value = System.getenv( key );
		if ( value != null && !value.isEmpty() ) {
			return value;
		}
		return null;
	}
}
