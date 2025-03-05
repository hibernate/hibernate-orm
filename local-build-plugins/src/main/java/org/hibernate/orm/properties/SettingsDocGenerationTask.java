/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.hibernate.build.OrmBuildDetails;
import org.hibernate.orm.properties.jdk17.SettingsCollector;

import javax.inject.Inject;

import static org.hibernate.orm.properties.SettingsDocumentationPlugin.TASK_GROUP_NAME;

/**
 * @author Steve Ebersole
 */
public class SettingsDocGenerationTask extends DefaultTask {
	public static final String TASK_NAME = "generateSettingsDoc";

	private final DirectoryProperty javadocDirectory;
	private final Property<String> publishedDocsUrl;
	private final Property<String> anchorNameBase;
	private final NamedDomainObjectContainer<SettingsDocSection> sections;

	private final RegularFileProperty outputFile;

	@Inject
	public SettingsDocGenerationTask(SettingsDocExtension dslExtension, Project project) {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Collects descriptions of Hibernate configuration properties in preparation for inclusion in the User Guide" );

		getInputs().property( "ormVersion", getProject().getExtensions().getByType( OrmBuildDetails.class ).getHibernateVersion() );

		javadocDirectory = project.getObjects().directoryProperty();
		javadocDirectory.convention( dslExtension.getJavadocDirectory() );

		publishedDocsUrl = project.getObjects().property( String.class );
		publishedDocsUrl.convention( dslExtension.getPublishedDocsUrl() );

		anchorNameBase = project.getObjects().property( String.class );
		anchorNameBase.convention( dslExtension.getAnchorNameBase() );

		sections = dslExtension.getSections();

		outputFile = project.getObjects().fileProperty();
		outputFile.convention( dslExtension.getOutputFile() );
	}

	@InputDirectory
	@IgnoreEmptyDirectories
	public DirectoryProperty getJavadocDirectory() {
		return javadocDirectory;
	}

	@Input
	public Property<String> getPublishedDocsUrl() {
		return publishedDocsUrl;
	}

	@Input
	public Property<String> getAnchorNameBase() {
		return anchorNameBase;
	}

	@Nested
	public NamedDomainObjectContainer<SettingsDocSection> getSections() {
		return sections;
	}

	@OutputFile
	public RegularFileProperty getOutputFile() {
		return outputFile;
	}

	@TaskAction
	public void generateSettingsDocumentation() {
		final String publishedJavadocUrl = publishedDocsUrl.get()
				+ "/"
				+ getProject().getExtensions().getByType( OrmBuildDetails.class ).getHibernateVersionFamily()
				+ "/javadocs/";

		AsciiDocWriter.writeToFile(
				anchorNameBase.get(),
				SettingsCollector.collectSettingDescriptors(
						javadocDirectory.get(),
						sections.getAsMap(),
						publishedJavadocUrl
				),
				outputFile.get(),
				getProject()
		);
	}
}
