/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties;

import java.util.Comparator;

import static java.util.Comparator.comparing;

/**
 * DSL extension for defining a section in the settings appendix in the User Guide.
 * <p/>
 * Specifies the settings class to match, and identifies which module (by name) the
 * settings class from.
 *
 * @author Steve Ebersole
 */
public class SettingsDocSection {
	public static final Comparator<SettingsDocSection> BY_NAME = comparing( SettingsDocSection::getName );

	/**
	 * Factory for SettingsDocSection instances
	 */
	public static SettingsDocSection create(String name) {
		return new SettingsDocSection( name );
	}

	private final String name;

	// todo : do we ever care about multiple settings-classes for a single project?
	private String projectPath;
	private String settingsClassName;

	public SettingsDocSection(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}

	public String getSettingsClassName() {
		return settingsClassName;
	}

	public void setSettingsClassName(String settingsClassName) {
		this.settingsClassName = settingsClassName;
	}
}
