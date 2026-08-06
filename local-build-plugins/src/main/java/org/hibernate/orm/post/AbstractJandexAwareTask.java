/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;

import org.hibernate.build.OrmBuildDetails;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJandexAwareTask extends DefaultTask {
	private final Provider<IndexManager> indexManager;

	public AbstractJandexAwareTask() {
		setGroup( TASK_GROUP_NAME );

		this.indexManager = getProject().provider( () -> getProject().getExtensions().getByType( IndexManager.class ) );
		getInputs().property(
				"version",
				getProject().provider(
						() -> getProject().getExtensions().getByType( OrmBuildDetails.class ).getHibernateVersion()
				)
		);
	}

	@Internal
	protected abstract Provider<RegularFile> getTaskReportFileReference();

	@Internal
	protected IndexManager getIndexManager() {
		return indexManager.get();
	}

	@InputFile
	public Provider<RegularFile> getIndexFileReference() {
		return indexManager.get().getIndexFileReferenceAccess();
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReference() {
		return getTaskReportFileReference();
	}

	protected File prepareReportFile() {
		final File reportFile = getReportFileReference().get().getAsFile();

		if ( reportFile.getParentFile().exists() ) {
			if ( reportFile.exists() ) {
				if ( !reportFile.delete() ) {
					throw new RuntimeException( "Unable to delete report file - " + reportFile.getAbsolutePath() );
				}
			}
		}
		else {
			if ( !reportFile.getParentFile().mkdirs() ) {
				throw new RuntimeException( "Unable to create report file directories - " + reportFile.getAbsolutePath() );
			}
		}

		try {
			if ( !reportFile.createNewFile() ) {
				throw new RuntimeException( "Unable to create report file - " + reportFile.getAbsolutePath() );
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create report file - " + reportFile.getAbsolutePath() );
		}

		return reportFile;
	}

}
