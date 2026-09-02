/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;

/// Resolves and validates the Hibernate version represented by a provider's
/// Gradle dependency graph.
///
/// @author Steve Ebersole
public final class HibernateVersions {
	private HibernateVersions() {
	}

	public static String resolve(Configuration... configurations) {
		final Set<String> versions = new LinkedHashSet<>();
		for ( Configuration configuration : configurations ) {
			for ( ResolvedComponentResult component : configuration.getIncoming()
					.getResolutionResult()
					.getAllComponents() ) {
				if ( component.getId() instanceof ModuleComponentIdentifier module
						&& "org.hibernate.orm".equals( module.getGroup() )
						&& "hibernate-core".equals( module.getModule() ) ) {
					versions.add( module.getVersion() );
				}
			}
		}
		if ( versions.isEmpty() ) {
			throw new GradleException(
					"No org.hibernate.orm:hibernate-core component was found on the provider's main classpath"
			);
		}
		if ( versions.size() != 1 ) {
			throw new GradleException( "Conflicting Hibernate ORM Core versions were resolved: " + versions );
		}
		return versions.iterator().next();
	}

	public static String family(String version) {
		if ( version == null ) {
			throw new GradleException( "A Hibernate ORM version is required" );
		}
		final String[] segments = version.trim().split( "\\." );
		if ( segments.length < 2 || !digits( segments[0] ) || !digits( segments[1] ) ) {
			throw new GradleException(
					"Cannot derive a Hibernate ORM release family from version '" + version + "'; expected X.Y[.Z]"
			);
		}
		return segments[0] + "." + segments[1];
	}

	public static void verifyFamily(String pluginVersion, String coreVersion) {
		final String pluginFamily = family( pluginVersion );
		final String coreFamily = family( coreVersion );
		if ( !pluginFamily.equals( coreFamily ) ) {
			throw new GradleException(
					"Hibernate Dialect provider plugin " + pluginVersion + " supports the " + pluginFamily
							+ " release family, but hibernate-core " + coreVersion + " belongs to " + coreFamily
			);
		}
	}

	private static boolean digits(String value) {
		if ( value.isEmpty() ) {
			return false;
		}
		for ( int i = 0; i < value.length(); i++ ) {
			if ( !Character.isDigit( value.charAt( i ) ) ) {
				return false;
			}
		}
		return true;
	}
}
