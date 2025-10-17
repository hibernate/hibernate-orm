/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
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
	private final Property<OrmBuildDetails> ormBuildDetails;

	private final RegularFileProperty outputFile;

	@Inject
	public SettingsDocGenerationTask(SettingsDocExtension dslExtension, ObjectFactory objects) {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Collects descriptions of Hibernate configuration properties in preparation for inclusion in the User Guide" );

		ormBuildDetails = objects.property( OrmBuildDetails.class );
		getInputs().property( "ormVersion", ormBuildDetails.map( OrmBuildDetails::getHibernateVersion ) );

		javadocDirectory = objects.directoryProperty();
		javadocDirectory.convention( dslExtension.getJavadocDirectory() );

		publishedDocsUrl = objects.property( String.class );
		publishedDocsUrl.convention( dslExtension.getPublishedDocsUrl() );

		anchorNameBase = objects.property( String.class );
		anchorNameBase.convention( dslExtension.getAnchorNameBase() );

		sections = dslExtension.getSections();

		outputFile = objects.fileProperty();
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

	@Nested
	public Property<OrmBuildDetails> getOrmBuildDetails() {
		return ormBuildDetails;
	}

	@OutputFile
	public RegularFileProperty getOutputFile() {
		return outputFile;
	}

	@TaskAction
	public void generateSettingsDocumentation() {
		final String publishedJavadocUrl = publishedDocsUrl.get()
				+ "/"
				+ ormBuildDetails.get().getHibernateVersionFamily()
				+ "/javadocs/";

		AsciiDocWriter.writeToFile(
				anchorNameBase.get(),
				SettingsCollector.collectSettingDescriptors(
						javadocDirectory.get(),
						sections.getAsMap(),
						publishedJavadocUrl
				),
				outputFile.get()
		);
	}
}
