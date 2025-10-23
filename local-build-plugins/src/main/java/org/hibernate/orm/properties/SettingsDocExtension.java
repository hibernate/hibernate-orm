/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.properties;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * DSL extension for configuring aspects of the settings documentation process
 *
 * @author Steve Ebersole
 */
public class SettingsDocExtension {
	public static final String EXTENSION_NAME = "settingsDocumentation";

	private final DirectoryProperty javadocDirectory;
	private final Property<String> publishedDocsUrl;
	private final Property<String> anchorNameBase;
	private final NamedDomainObjectContainer<SettingsDocSection> sections;

	private final RegularFileProperty outputFile;

	@Inject
	public SettingsDocExtension(ObjectFactory objects) {
		javadocDirectory = objects.directoryProperty();
		publishedDocsUrl = objects.property( String.class );
		anchorNameBase = objects.property( String.class );
		sections = objects.domainObjectContainer( SettingsDocSection.class, SettingsDocSection::create );

		outputFile = objects.fileProperty();
	}

	/**
	 * The local directory which contains the Javadoc to be processed.
	 * <p/>
	 * Defaults to {@code ${build-dir}/javadocs}
	 */
	public DirectoryProperty getJavadocDirectory() {
		return javadocDirectory;
	}

	/**
	 * The base URL for the published doc server.  This is used to
	 * replace local hrefs with hrefs on the doc sever
	 * <p/>
	 * Defaults to {@code https://docs.jboss.org/hibernate/orm}
	 */
	public Property<String> getPublishedDocsUrl() {
		return publishedDocsUrl;
	}

	public Property<String> getAnchorNameBase() {
		return anchorNameBase;
	}

	public void setAnchorNameBase(String base) {
		anchorNameBase.set( base );
	}

	/**
	 * Configuration of the sections within the generated document
	 */
	public NamedDomainObjectContainer<SettingsDocSection> getSections() {
		return sections;
	}

	/**
	 * @see #getSections()
	 */
	public void sections(Action<NamedDomainObjectContainer<SettingsDocSection>> action) {
		action.execute( getSections() );
	}

	/**
	 * The file where the settings doc should be written
	 * <p/>
	 * Defaults to {@code ${build-dir}/asciidoc/fragments/config-settings.adoc}
	 */
	public RegularFileProperty getOutputFile() {
		return outputFile;
	}
}
