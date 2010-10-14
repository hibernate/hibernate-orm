/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.build.gradle.maven;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Collections;

import org.apache.maven.settings.DefaultMavenSettingsBuilder;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.publish.maven.pombuilder.PlexusLoggerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class LocalMavenRepoSniffer implements Plugin<Project> {
	private static final Logger log = LoggerFactory.getLogger( LocalMavenRepoSniffer.class );

	private final File userHome = new File( System.getProperty( "user.home" ) );

	@Override
	public void apply(Project project) {
		File mavenLocal = new File( new File( userHome, ".m2" ), "repository" );

		File userSettings = new File( new File( System.getProperty( "user.home" ), ".m2" ), "settings.xml" );
		if ( userSettings.exists() ) {
			File overriddenMavenLocal = extractMavenLocal( userSettings );
			if ( overriddenMavenLocal != null ) {
				mavenLocal = overriddenMavenLocal;
			}
		}

		try {
			project.getLogger().trace( "Adding local maven repo [" + mavenLocal.toString() + "]" );
// adam: here is where mavenLocal() would come in...
			project.getRepositories().mavenRepo(
					Collections.singletonMap( "urls", mavenLocal.toURI().toURL().toExternalForm() )
			);
		}
		catch ( MalformedURLException e ) {
			project.getLogger().warn( "Unable to process local maven repo url", e );
		}

	}

	private static final String USER_HOME_MARKER = "${user.home}/";

	private File extractMavenLocal(File userSettings) {
		Settings settings = extractSettings( userSettings );
		String override = settings.getLocalRepository();
		if ( override != null ) {
			override = override.trim();
			if ( override.length() > 0 ) {
				// Nice, it does not even handle the interpolation for us, so we'll handle some common cases...
				if ( override.startsWith( USER_HOME_MARKER ) ) {
					override = userHome.getAbsolutePath() + '/' + override.substring( USER_HOME_MARKER.length() );
				}
				return new File( override );
			}
		}
		return null;
	}

	private Settings extractSettings(File userSettings) {
		try {
			MavenSettingsBuilder builder = buildSettingsBuilder( userSettings );
			return builder.buildSettings();
		}
		catch ( Exception e ) {
			log.debug( "Unable to build Maven settings : " + e );
		}
		return null;
	}

	private MavenSettingsBuilder buildSettingsBuilder(File userSettings) throws Exception {
		final String userSettingsPath = userSettings.getAbsolutePath();

		DefaultMavenSettingsBuilder builder = new DefaultMavenSettingsBuilder();
		builder.enableLogging( new PlexusLoggerAdapter( log ) );

		Field userSettingsPathField = DefaultMavenSettingsBuilder.class.getDeclaredField( "userSettingsPath" );
		userSettingsPathField.setAccessible( true );
		userSettingsPathField.set( builder, userSettingsPath );

		Field globalSettingsPathField = DefaultMavenSettingsBuilder.class.getDeclaredField( "globalSettingsPath" );
		globalSettingsPathField.setAccessible( true );
		globalSettingsPathField.set( builder, userSettingsPath );

		builder.initialize();

		return builder;
	}
}
