/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.gradle.api.GradleException;

/// Loads versions embedded in the published Dialect-provider plugin.
///
/// @author Steve Ebersole
public final class PluginVersions {
	private static final Properties VERSIONS = load();

	private PluginVersions() {
	}

	public static String hibernateOrm() {
		return VERSIONS.getProperty( "hibernateOrm" );
	}

	public static String junitJupiter() {
		return VERSIONS.getProperty( "junitJupiter" );
	}

	public static String junitPlatform() {
		return VERSIONS.getProperty( "junitPlatform" );
	}

	private static Properties load() {
		final Properties properties = new Properties();
		try ( InputStream stream = PluginVersions.class.getResourceAsStream( "/hibernate-dialect-provider.properties" ) ) {
			if ( stream == null ) {
				throw new GradleException( "Missing hibernate-dialect-provider.properties plugin resource" );
			}
			properties.load( stream );
			return properties;
		}
		catch (IOException e) {
			throw new GradleException( "Unable to read Dialect-provider plugin version metadata", e );
		}
	}
}
