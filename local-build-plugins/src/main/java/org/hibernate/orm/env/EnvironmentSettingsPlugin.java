/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.env;

import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.hibernate.build.JpaVersion;

import java.util.Map;
import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class EnvironmentSettingsPlugin implements Plugin<Settings> {

	@Override
	public void apply(Settings settings) {
		final JpaVersion jpaVersion = JpaVersion.from( settings );
		settings.getExtensions().add( "jpaVersion", jpaVersion );
		settings.getExtensions().add( JpaVersion.EXT_KEY, jpaVersion.getOsgiName() );
		settings.getExtensions().add( "db", Objects.requireNonNullElse( getP( settings, "db" ), "h2" ) );
		String ciNode = getP( settings, "ci.node" );
		if ( ciNode != null ) {
			settings.getExtensions().add( "ci.node", ciNode );
		}
	}

	private static String getP(Settings settings, String key) {
		StartParameter startParameter = settings.getStartParameter();
		// the `-P` settings passed at command-line
		final Map<String, String> projectProperties = startParameter.getProjectProperties();
		final String projectProperty = projectProperties.get( key );
		if ( projectProperty != null && !projectProperty.isEmpty() ) {
			return projectProperty;
		}
		else {
			return null;
		}
	}

}
