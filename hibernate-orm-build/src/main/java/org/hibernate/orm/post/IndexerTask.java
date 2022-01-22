/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Task for creating Jandex Index within Gradle UP-TO-DATE handling
 *
 * @author Steve Ebersole
 */
public abstract class IndexerTask extends DefaultTask {
	private final IndexManager indexManager;

	@Inject
	public IndexerTask(IndexManager indexManager) {
		this.indexManager = indexManager;
	}

	@InputFile
	public Provider<RegularFile> getJarFileReference() {
		return indexManager.getJarFileReference();
	}

	@OutputFile
	public Provider<RegularFile> getIndexFileReference() {
		return indexManager.getIndexFileReference();
	}

	@TaskAction
	public void createIndex() {
		indexManager.index();
	}
}
