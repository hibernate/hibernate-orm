/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.orm.ReleaseFamilyIdentifier;

/**
 * @author Steve Ebersole
 */
public abstract class PublishMigrationGuide extends DefaultTask {
	public static final String NAME = "publishMigrationGuide";

	private final Provider<Object> projectVersion;
	private final Property<ReleaseFamilyIdentifier> currentlyBuildingFamily;
	private final Property<String> docServerUrl;
	private final DirectoryProperty migrationGuideDirectory;

	public PublishMigrationGuide() {
		setGroup( "documentation" );
		setDescription( "Publishes the migration-guide associated with the current branch. " +
				"Intended for incremental publishing of the guide for corrections, etc. without doing a full release. " +
				"Note that this is not needed when doing a release as the migration-guide is published as part of that workflow." );

		getInputs().property( "hibernate-version", getProject().getVersion() );

		projectVersion = getProject().provider( () -> getProject().getVersion() );
		currentlyBuildingFamily = getProject().getObjects().property( ReleaseFamilyIdentifier.class );

		docServerUrl = getProject().getObjects().property( String.class );
		migrationGuideDirectory = getProject().getObjects().directoryProperty();
	}

	@Input
	public Provider<Object> getProjectVersion() {
		return projectVersion;
	}

	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	public DirectoryProperty getMigrationGuideDirectory() {
		return migrationGuideDirectory;
	}

	@Input
	public Property<String> getDocServerUrl() {
		return docServerUrl;
	}


	@Input
	public Property<ReleaseFamilyIdentifier> getCurrentlyBuildingFamily() {
		return currentlyBuildingFamily;
	}

	@TaskAction
	public void uploadMigrationGuide() {
		final String base = docServerUrl.get();
		final String normalizedBase = base.endsWith( "/" ) ? base : base + "/";
		final String url = normalizedBase + currentlyBuildingFamily.get().toExternalForm() + "/migration-guide/";

		RsyncHelper.rsync( migrationGuideDirectory.get(), url, getProject() );
	}
}
