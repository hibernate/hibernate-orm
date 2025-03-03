/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;

import org.hibernate.orm.ReleaseFamilyIdentifier;

/**
 * @author Steve Ebersole
 */
public abstract class PublishTask extends DefaultTask {
	public static final String UPLOAD_TASK_NAME = "uploadDocumentation";

	private final Property<ReleaseFamilyIdentifier> buildingFamily;
	private final Property<String> docServerUrl;
	private final DirectoryProperty stagingDirectory;

	public PublishTask() {
		setGroup( "documentation" );
		setDescription( "Publish documentation to the doc server" );

		buildingFamily = getProject().getObjects().property( ReleaseFamilyIdentifier.class );
		docServerUrl = getProject().getObjects().property( String.class );
		stagingDirectory = getProject().getObjects().directoryProperty();
	}

	@Input
	public Property<String> getDocServerUrl() {
		return docServerUrl;
	}

	@Input
	public Property<ReleaseFamilyIdentifier> getBuildingFamily() {
		return buildingFamily;
	}

	@InputDirectory
	public Property<Directory> getStagingDirectory() {
		return stagingDirectory;
	}

	@TaskAction
	public void uploadDocumentation() {
		final String releaseFamily = buildingFamily.get().toExternalForm();
		final String base = docServerUrl.get();
		final String normalizedBase = base.endsWith( "/" ) ? base : base + "/";
		final String url = normalizedBase + releaseFamily;

		final String stagingDirPath = stagingDirectory.get().getAsFile().getAbsolutePath();
		final String stagingDirPathContent =  stagingDirPath.endsWith( "/" ) ? stagingDirPath : stagingDirPath + "/";

		getProject().getLogger().lifecycle( "Uploading documentation `{}` -> `{}`", stagingDirPath, url );
		final ExecResult result = getProject().exec( (exec) -> {
			exec.executable( "rsync" );
			exec.args("-avz", "--delete", stagingDirPathContent, url );
		} );
		getProject().getLogger().lifecycle( "Done uploading documentation - {}", result.getExitValue() == 0 ? "success" : "failure" );
		setDidWork( true );
	}
}
