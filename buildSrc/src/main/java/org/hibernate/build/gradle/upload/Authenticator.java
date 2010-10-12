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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.ant.Authentication;
import org.apache.maven.artifact.ant.RemoteRepository;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Responsible for locating and injecting authentication information into the JBoss Nexus repository config for upload
 * which it does, based on set up in
 *
 * @author Steve Ebersole
 */
public class Authenticator extends DefaultTask {
	private Set<RemoteRepository> nexusRepositories = new HashSet<RemoteRepository>();

	void addRepository(RemoteRepository remoteRepository) {
		nexusRepositories.add( remoteRepository );
	}

	@TaskAction
	public void configureNexusAuthentication() {
		// See if we have username/password...
		Authentication authentication = locateAuthenticationDetails();
		if ( authentication == null ) {
			if ( ! nexusRepositories.isEmpty() ) {
				getLogger().warn( "Unable to locate JBoss Nexus username/password; upload will most likely fail" );
			}
			return;
		}

		for ( RemoteRepository remoteRepository : nexusRepositories ) {
			remoteRepository.addAuthentication( authentication );
		}
	}

	public static final String ALT_PROP_FILE_NAME = "jboss-nexus.properties";
	public static final String USER = "JBOSS_NEXUS_USERNAME";
	public static final String PASS = "JBOSS_NEXUS_PASSWORD";

	private Authentication locateAuthenticationDetails() {
		String username = (String) getProject().getProperties().get( USER );
		String password = (String) getProject().getProperties().get( PASS );

		if ( username != null && password != null ) {
			return newAuthentication( username, password );
		}

		File alternateFile = new File( new File( System.getProperty( "user.home" ), ".gradle" ), ALT_PROP_FILE_NAME );
		Properties alternateProperties = new Properties();
		// in case one or the other were specified...
		if ( username != null )  {
			alternateProperties.put( USER, username );
		}
		if ( password != null ) {
			alternateProperties.put( PASS, password );
		}
		try {
			FileInputStream fis = new FileInputStream( alternateFile );
			try {
				alternateProperties.load( fis );
			}
			catch ( IOException e ) {
				getLogger().debug( "Unable to load alternate JBoss Nexus properties file", e );
			}
			finally {
				try {
					fis.close();
				}
				catch ( IOException ignore ) {
				}
			}
		}
		catch ( FileNotFoundException e ) {
			getLogger().debug( "Unable to locate alternate JBoss Nexus properties file" );
		}

		username = alternateProperties.getProperty( USER );
		password = alternateProperties.getProperty( PASS );
		if ( username != null && password != null ) {
			return newAuthentication( username, password );
		}

		return null;
	}

	private Authentication newAuthentication(String username, String password) {
		Authentication authentication = new Authentication();
		authentication.setUserName( username );
		authentication.setPassword( password );
		return authentication;
	}
}
