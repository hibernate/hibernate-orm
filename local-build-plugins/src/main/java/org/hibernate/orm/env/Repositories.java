/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.env;

import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor;

import java.net.URI;

/**
 * Adds optional Maven repositories (mavenLocal, Maven Central Snapshots).
 */
public class Repositories {

	/**
	 * Adds a {@code mavenLocal()} repository if the {@code enableMavenLocalRepo} system property is set.
	 *
	 * @param contentFilter optional content filter, e.g. {@code c -> c.includeGroup("jakarta.persistence")}
	 */
	public static void maybeAddMavenLocal(RepositoryHandler repositories, Action<RepositoryContentDescriptor> contentFilter) {
		if ( "true".equalsIgnoreCase( resolve( "enableMavenLocalRepo" ) ) ) {
			repositories.mavenLocal( repo -> {
				if ( contentFilter != null ) {
					repo.content( contentFilter );
				}
			} );
		}
	}

	public static void maybeAddMavenLocal(RepositoryHandler repositories) {
		maybeAddMavenLocal( repositories, null );
	}

	/**
	 * Adds the Maven Central Snapshots repository.
	 * Useful for running against current snapshots of Jakarta APIs.
	 *
	 * @param contentFilter optional content filter, e.g. {@code c -> c.includeGroup("jakarta.persistence")}
	 */
	public static void maybeAddMavenCentralSnapshots(RepositoryHandler repositories, Action<RepositoryContentDescriptor> contentFilter) {
		repositories.maven( repo -> {
			repo.setName( "Central Portal Snapshots" );
			repo.setUrl( URI.create( "https://central.sonatype.com/repository/maven-snapshots/" ) );
			if ( contentFilter != null ) {
				repo.content( contentFilter );
			}
		} );
	}

	public static void maybeAddMavenCentralSnapshots(RepositoryHandler repositories) {
		maybeAddMavenCentralSnapshots( repositories, null );
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
