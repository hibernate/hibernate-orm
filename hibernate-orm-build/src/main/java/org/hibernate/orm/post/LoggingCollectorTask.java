/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.Index;

/**
 * @author Steve Ebersole
 */
public abstract class LoggingCollectorTask extends DefaultTask {
	private final IndexManager indexManager;

	@Inject
	public LoggingCollectorTask(IndexManager indexManager) {
		this.indexManager = indexManager;
	}

	@TaskAction
	public void collectLoggingDetails() {
		getProject().getLogger().lifecycle( "Logging collection not implemented yet" );
	}
}
