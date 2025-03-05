/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.properties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

/**
 * DSL extension for defining a section in the settings appendix in the User Guide.
 *
 * @author Steve Ebersole
 */
public class SettingsDocSection {
	private final String name;

	private Integer explicitPosition;
	private String summary;
	private String description;
	private List<String> settingsClassNames = new ArrayList<>();

	public SettingsDocSection(String name) {
		this.name = name;
	}

	private SettingsDocSection(
			String name,
			Integer explicitPosition) {
		this.name = name;
		this.explicitPosition = explicitPosition;
	}

	@Internal
	public String getName() {
		return name;
	}

	@Optional
	@Input
	public Integer getExplicitPosition() {
		return explicitPosition;
	}

	public void setExplicitPosition(Integer explicitPosition) {
		this.explicitPosition = explicitPosition;
	}

	@Input
	public List<String> getSettingsClassNames() {
		return settingsClassNames;
	}

	public void setSettingsClassNames(List<String> settingsClassNames) {
		this.settingsClassNames = settingsClassNames;
	}

	public void settingsClassName(String name) {
		settingsClassNames.add( name );
	}

	@Input
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Input
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "SettingsDocSection(" + name + " [" + explicitPosition + "])";
	}

	/**
	 * Factory for SettingsDocSection instances
	 */
	public static SettingsDocSection create(String name) {
		return new SettingsDocSection( name );
	}

	private static final Comparator<Integer> nullsLastInComparator = Comparator.nullsLast( Integer::compare );
	private static final Comparator<SettingsDocSection> nameComparator = Comparator.comparing( SettingsDocSection::getName );

	public static int compare(SettingsDocSection section1, SettingsDocSection section2) {
		// todo (settings-doc) : add support for negative-as-last?
		//		- as a means to easily sort "less used" values at the end (EnvironmentSettings, etc)

		final Integer explicitPosition1 = section1.getExplicitPosition();
		final Integer explicitPosition2 = section2.getExplicitPosition();

		final int positionComparison = nullsLastInComparator.compare( explicitPosition1, explicitPosition2 );

		if ( positionComparison != 0 ) {
			return positionComparison;
		}

		return nameComparator.compare( section1, section2 );
	}
}
