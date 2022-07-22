/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;

import org.hibernate.orm.ReleaseFamilyIdentifier;

/**
 * @author Steve Ebersole
 */
public abstract class PublishTask extends DefaultTask {
	private final ReleaseFamilyIdentifier buildingFamily;
	private final Provider<String> docServerUrl;
	private final Provider<Directory> stagingDirectory;

	@Inject
	public PublishTask(DocumentationPublishing config) {
		setGroup( "Release" );
		setDescription( "Publish documentation to the doc server" );

		buildingFamily = config.getReleaseFamilyIdentifier();
		stagingDirectory = config.getStagingDirectory();
		docServerUrl = config.getDocServerUrl();
	}

	@Input
	public Provider<String> getDocServerUrl() {
		return docServerUrl;
	}

	@InputDirectory
	public Provider<Directory> getStagingDirectory() {
		return stagingDirectory;
	}

	@TaskAction
	public void uploadDocumentation() {
		final String releaseFamily = buildingFamily.toExternalForm();
		final String base = docServerUrl.get();
		final String normalizedBase = base.endsWith( "/" ) ? base : base + "/";
		final String url = normalizedBase + releaseFamily;

		final String stagingDirPath = stagingDirectory.get().getAsFile().getAbsolutePath();
		final String stagingDirPathContent =  stagingDirPath.endsWith( "/" ) ? stagingDirPath : stagingDirPath + "/";

		getProject().getLogger().lifecycle( "Uploading documentation `{}` -> `{}`", stagingDirPath, url );
		final ExecResult result = getProject().exec( (exec) -> {
			exec.executable( "rsync" );
			exec.args("--port=2222", "-avz", "--links", "--delete", stagingDirPathContent, url );
		} );
		getProject().getLogger().lifecycle( "Done uploading documentation - {}", result.getExitValue() == 0 ? "success" : "failure" );
	}
}
