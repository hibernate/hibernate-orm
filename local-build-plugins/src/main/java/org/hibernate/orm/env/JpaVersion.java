/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.env;

import java.util.Map;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

/**
 * @author Steve Ebersole
 */
public class JpaVersion {
	public static final String EXT_KEY = "jakartaJpaVersion";
	public static final String VERSION_KEY = "jakartaJpaVersionOverride";
	public static final String DEFAULT_VERSION = "3.2.0-M2";

	private final String name;
	private final String osgiName;

	public JpaVersion(String version) {
		this.name = version;
		this.osgiName = version + ".0";
	}

	public static JpaVersion from(Settings settings) {
		return from( settings.getStartParameter() );
	}

	private static JpaVersion from(StartParameter startParameter) {
		// the `-P` settings passed at command-line
		final Map<String, String> projectProperties = startParameter.getProjectProperties();
		final String projectProperty = projectProperties.get( VERSION_KEY );
		if ( projectProperty != null && !projectProperty.isEmpty() ) {
			// use this one...
			return new JpaVersion( projectProperty );
		}

		return new JpaVersion( System.getProperty( VERSION_KEY, DEFAULT_VERSION ) );
	}

	public static JpaVersion from(Project project) {
		return from( project.getGradle().getStartParameter() );
	}

	public String getName() {
		return name;
	}

	public String getOsgiName() {
		return osgiName;
	}

	@Override
	public String toString() {
		return name;
	}
}
