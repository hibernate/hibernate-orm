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

import org.apache.maven.artifact.ant.Authentication;
import org.apache.maven.artifact.ant.RemoteRepository;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.maven.MavenDeployer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.Upload;

/**
 * Responsible for locating and injecting authentication information into the JBoss Nexus repository config for upload
 * which it does, based on set up in
 *
 * @author Steve Ebersole
 */
public class AuthenticationHandler extends DefaultTask {
	private AuthenticationProviderRegistry authenticationProviderRegistry;
	private Upload uploadTask;

	public void injectProviderRegistry(AuthenticationProviderRegistry authenticationProviderRegistry) {
		this.authenticationProviderRegistry = authenticationProviderRegistry;
	}

	public void injectUploadTask(Upload uploadTask) {
		this.uploadTask = uploadTask;
	}

	@TaskAction
	public void configureUploadAuthentication() {
		// todo : unfortunately I have no idea how to apply this to non MavenDeployer-type repos...
		uploadTask.getRepositories().withType( MavenDeployer.class ).all(
				new Action<MavenDeployer>() {
					public void execute(MavenDeployer deployer) {
						final RemoteRepository repository =  deployer.getRepository();
						if ( repository != null ) {
							final Authentication authentication = locateAuthenticationDetails( repository );
							if ( authentication != null ) {
								repository.addAuthentication( authentication );
							}
						}
						final RemoteRepository snapshotRepository = deployer.getSnapshotRepository();
						if ( snapshotRepository != null ) {
							final Authentication authentication = locateAuthenticationDetails( snapshotRepository );
							if ( authentication != null ) {
								snapshotRepository.addAuthentication( authentication );
							}
						}
					}
				}
		);
	}

	private Authentication locateAuthenticationDetails(RemoteRepository repository) {
		for ( AuthenticationProvider provider : authenticationProviderRegistry.providers() ) {
			Authentication authentication = provider.determineAuthentication( repository );
			if ( authentication != null ) {
				return authentication;
			}
		}
		return null;
	}
}
