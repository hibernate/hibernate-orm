/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.tasks.Jar;

/**
 * @author Steve Ebersole
 */
public class CollectorPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		final Jar jarTask = (Jar) project.getTasks().findByName( "jar" );
		final Provider<RegularFile> jarFileReferenceAccess = jarTask.getArchiveFile();
		final Provider<RegularFile> indexFileReferenceAccess = project.getLayout()
				.getBuildDirectory()
				.file( "post/" + project.getName() + ".idx" );

		final IndexManager indexManager = new IndexManager( jarFileReferenceAccess, indexFileReferenceAccess );

		final IndexerTask indexerTask = project.getTasks().create(
				"indexProjectJar",
				IndexerTask.class,
				indexManager
		);

		final IncubatingCollectorTask incubatingTask = project.getTasks().create(
				"collectProjectIncubating",
				IncubatingCollectorTask.class,
				indexManager
		);
		incubatingTask.dependsOn( indexerTask );

		final LoggingCollectorTask loggingTask = project.getTasks().create(
				"collectProjectLogging",
				LoggingCollectorTask.class,
				indexManager
		);
		loggingTask.dependsOn( indexerTask );
	}
}
