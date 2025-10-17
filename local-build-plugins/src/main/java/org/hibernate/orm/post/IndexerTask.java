/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

import static org.hibernate.orm.post.ReportGenerationPlugin.AGGREGATE_CONFIG_NAME;
import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * Task for creating Jandex Index within Gradle UP-TO-DATE handling
 *
 * @author Steve Ebersole
 */
public abstract class IndexerTask extends DefaultTask {
	private final ArchiveOperations archiveOperations;

	@Inject
	public IndexerTask(ArchiveOperations archiveOperations) {
		this.archiveOperations = archiveOperations;
		setGroup( TASK_GROUP_NAME );
		setDescription( String.format( "Builds a Jandex Index from the artifacts attached to the `%s` Configuration", AGGREGATE_CONFIG_NAME ) );
	}

	@Nested
	public abstract Property<IndexManager> getIndexManager();

	@TaskAction
	public void createIndex() {
		getIndexManager().get().index( archiveOperations, getLogger() );
	}
}
