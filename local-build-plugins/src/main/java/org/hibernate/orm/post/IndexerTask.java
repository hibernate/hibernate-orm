/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import static org.hibernate.orm.post.ReportGenerationPlugin.CONFIG_NAME;
import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * Task for creating Jandex Index within Gradle UP-TO-DATE handling
 *
 * @author Steve Ebersole
 */
public abstract class IndexerTask extends DefaultTask {
	private final Provider<IndexManager> indexManager;

	public IndexerTask() {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Builds a Jandex Index from the artifacts attached to the `" + CONFIG_NAME + "` Configuration" );

		indexManager = getProject().provider( () -> getProject().getExtensions().getByType( IndexManager.class ) );
	}

	@InputFiles
	@SkipWhenEmpty
	public Configuration getArtifactsToProcess() {
		return indexManager.get().getArtifactsToProcess();
	}

	@OutputFile
	public Provider<RegularFile> getIndexFileReference() {
		return indexManager.get().getIndexFileReferenceAccess();
	}

	@OutputFile
	public Provider<RegularFile> getPackageFileReferenceAccess() {
		return indexManager.get().getPackageFileReferenceAccess();
	}

	@TaskAction
	public void createIndex() {
		indexManager.get().index();
	}
}
