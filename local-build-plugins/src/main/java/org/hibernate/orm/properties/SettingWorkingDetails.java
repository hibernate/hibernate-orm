/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SettingWorkingDetails {
	private final String name;
	private final String settingsClassName;
	private final String settingFieldName;
	private final String publishedJavadocLink;

	private String defaultValue;
	private String apiNote;
	private String since;
	private boolean deprecated;
	private boolean incubating;
	private boolean unsafe;
	private boolean compatibility;
	private List<String> relatedSettingNames;

	public SettingWorkingDetails(
			String name,
			String settingsClassName,
			String settingFieldName,
			String publishedJavadocLink) {
		this.name = name;
		this.settingsClassName = settingsClassName;
		this.settingFieldName = settingFieldName;
		this.publishedJavadocLink = publishedJavadocLink;
	}

	public String getName() {
		return name;
	}

	public String getSettingsClassName() {
		return settingsClassName;
	}

	public String getSettingFieldName() {
		return settingFieldName;
	}

	public String getPublishedJavadocLink() {
		return publishedJavadocLink;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getApiNote() {
		return apiNote;
	}

	public void setApiNote(String apiNote) {
		this.apiNote = apiNote;
	}

	public String getSince() {
		return since;
	}

	public void setSince(String since) {
		this.since = since;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

	public boolean isIncubating() {
		return incubating;
	}

	public void setIncubating(boolean incubating) {
		this.incubating = incubating;
	}

	public boolean isUnsafe() {
		return unsafe;
	}

	public void setUnsafe(boolean unsafe) {
		this.unsafe = unsafe;
	}

	public boolean isCompatibility() {
		return compatibility;
	}

	public void setCompatibility(boolean compatibility) {
		this.compatibility = compatibility;
	}

	public List<String> getRelatedSettingNames() {
		return relatedSettingNames;
	}

	public void addRelatedSettingName(String settingName) {
		if ( relatedSettingNames == null ) {
			relatedSettingNames = new ArrayList<>();
		}
		relatedSettingNames.add( settingName );
	}

	public SettingDescriptor buildDescriptor(String asciidoc) {
		if ( name == null ) {
			throw new IllegalStateException( "Setting name not specified" );
		}

		if ( settingsClassName == null ) {
			throw new IllegalStateException( "Setting constant class-name not specified" );
		}

		if ( settingFieldName == null ) {
			throw new IllegalStateException( "Setting constant field-name not specified" );
		}

		if ( publishedJavadocLink == null ) {
			throw new IllegalStateException( "Setting javadoc link not specified" );
		}

		if ( asciidoc == null ) {
			throw new IllegalStateException( "Setting comment not specified" );
		}

		return new SettingDescriptor(
				name,
				settingsClassName,
				settingFieldName,
				publishedJavadocLink,
				asciidoc,
				defaultValue,
				apiNote,
				since,
				deprecated,
				incubating,
				unsafe,
				compatibility
		);
	}
}
