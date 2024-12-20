/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties;

/**
 * @author Steve Ebersole
 */
public class SettingDescriptor {
	private final String name;
	private final String settingsClassName;
	private final String settingFieldName;
	private final String publishedJavadocLink;
	private final String comment;
	private final String defaultValue;
	private final String apiNote;
	private final LifecycleDetails lifecycleDetails;
	private final boolean unsafe;
	private final boolean compatibility;

	public SettingDescriptor(
			String name,
			String settingsClassName,
			String settingFieldName,
			String publishedJavadocLink,
			String comment,
			String defaultValue,
			String apiNote,
			boolean unsafe,
			boolean compatibility,
			LifecycleDetails lifecycleDetails) {
		this.name = name;
		this.settingsClassName = settingsClassName;
		this.settingFieldName = settingFieldName;
		this.comment = comment;
		this.publishedJavadocLink = publishedJavadocLink;
		this.defaultValue = defaultValue;
		this.apiNote = apiNote;
		this.unsafe = unsafe;
		this.compatibility = compatibility;
		this.lifecycleDetails = lifecycleDetails;
	}

	public SettingDescriptor(
			String name,
			String settingsClassName,
			String settingFieldName,
			String publishedJavadocLink,
			String comment,
			String defaultValue,
			String apiNote,
			String since,
			boolean deprecated,
			boolean incubating,
			boolean unsafe,
			boolean compatibility) {
		this(
				name,
				settingsClassName,
				settingFieldName,
				publishedJavadocLink, comment,
				defaultValue,
				apiNote,
				unsafe,
				compatibility,
				new LifecycleDetails( since, deprecated, incubating )
		);
	}

	/**
	 * The name of the setting
	 */
	public String getName() {
		return name;
	}

	/**
	 * The Javadoc content
	 */
	public String getComment() {
		return comment;
	}

	public String getPublishedJavadocLink() {
		return publishedJavadocLink;
	}

	/**
	 * The setting's default value
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * {@code @apiNote} defined on the setting field
	 */
	public String getApiNote() {
		return apiNote;
	}

	public String getSettingsClassName() {
		return settingsClassName;
	}

	public String getSettingFieldName() {
		return settingFieldName;
	}

	public boolean isUnsafe() {
		return unsafe;
	}

	public boolean isCompatibility() {
		return compatibility;
	}

	public LifecycleDetails getLifecycleDetails() {
		return lifecycleDetails;
	}

	public boolean hasMetadata() {
		return defaultValue != null
				|| apiNote != null
				|| lifecycleDetails.since != null
				|| lifecycleDetails.incubating
				|| lifecycleDetails.deprecated;
	}

	@Override
	public String toString() {
		return "setting(" + name + ") {\n" +
				"   settingsClassName = `" + settingsClassName + "`,\n" +
				"   settingFieldName = `" + settingFieldName + "`,\n" +
				"   lifecycle = " + lifecycleDetails + ",\n" +
				"   defaultValue = `" + defaultValue + "`,\n" +
				"   apiNote = `" + apiNote + "`,\n" +
				"   javadoc = ```\n" + comment + "\n```\n" +
				"}";
	}

	public static class LifecycleDetails {
		private final String since;
		private final boolean incubating;
		private final boolean deprecated;

		public LifecycleDetails(String since, boolean deprecated, boolean incubating) {
			this.since = since;
			this.deprecated = deprecated;
			this.incubating = incubating;
		}

		public String getSince() {
			return since;
		}

		public boolean isDeprecated() {
			return deprecated;
		}

		public boolean isIncubating() {
			return incubating;
		}

		@Override
		public String toString() {
			return "{\n" +
					"      since = `" + since + "`,\n" +
					"      incubating = `" + incubating + "`,\n" +
					"      deprecated = `" + deprecated + "`\n" +
					"   }";
		}
	}
}
