package org.hibernate.orm.docs;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
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

	private final Provider<String> docServerUrl;

	private final ReleaseFamilyIdentifier currentlyBuildingFamily;
	private final DirectoryProperty migrationGuideDirectory;

	@Inject
	public PublishMigrationGuide(DocumentationPublishing config) {
		setGroup( "Release" );
		setDescription( "Publishes the migration-guide associated with the current branch. " +
				"Intended for incremental publishing of the guide for corrections, etc. without doing a full release. " +
				"Note that this is not needed when doing a release as the migration-guide is published as part of that workflow." );

		getInputs().property( "hibernate-version", getProject().getVersion() );

		docServerUrl = config.getDocServerUrl();
		currentlyBuildingFamily = config.getReleaseFamilyIdentifier();
		migrationGuideDirectory = getProject().getObjects().directoryProperty();
	}

	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	public DirectoryProperty getMigrationGuideDirectory() {
		return migrationGuideDirectory;
	}

	@Input
	public Provider<String> getDocServerUrl() {
		return docServerUrl;
	}

	@TaskAction
	public void uploadMigrationGuide() {
		final String base = docServerUrl.get();
		final String normalizedBase = base.endsWith( "/" ) ? base : base + "/";
		final String url = normalizedBase + currentlyBuildingFamily.toExternalForm() + "/migration-guide/";

		RsyncHelper.rsync( migrationGuideDirectory.get(), url, getProject() );
	}
}
