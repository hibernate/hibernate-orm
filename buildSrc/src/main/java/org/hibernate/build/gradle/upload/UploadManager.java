/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.build.gradle.upload;

import org.apache.maven.artifact.ant.RemoteRepository;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.maven.MavenDeployer;
import org.gradle.api.tasks.Upload;

/**
 * Plugin to manage authentication
 *
 * @author Steve Ebersole
 */
public class UploadManager implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		final Authenticator authenticator = project.getTasks().add( "nexusAuthHandler", Authenticator.class );
		project.getTasks().withType( Upload.class ).allTasks(
			new Action<Upload>() {
				@Override
				public void execute(final Upload uploadTask) {
					uploadTask.getRepositories().withType( MavenDeployer.class ).allObjects(
							new Action<MavenDeployer>() {
								public void execute(MavenDeployer deployer) {
									RemoteRepository repository =  deployer.getRepository();
									if ( repository != null ) {
										authenticator.addRepository( repository );
										uploadTask.getDependsOn().add( authenticator );
									}
									repository = deployer.getSnapshotRepository();
									if ( repository != null ) {
										authenticator.addRepository( repository );
										uploadTask.getDependsOn().add( authenticator );
									}
								}
							}
					);
				}
			}
		);
	}
}
