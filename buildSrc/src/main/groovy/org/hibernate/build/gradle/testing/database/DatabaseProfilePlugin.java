/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.build.gradle.testing.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Plugin used to apply notion of database profiles, which are consumed by the matrix testing plugin.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class DatabaseProfilePlugin implements Plugin<Project> {
	/**
	 * The directory containing standard database profiles.
	 */
    public static final String STANDARD_DATABASES_DIRECTORY = "databases";
	/**
	 * Names a system setting key that can be set to point to a directory containing additional, custom
	 * database profiles.
	 */
	public static final String CUSTOM_DATABASES_DIRECTORY_KEY = "hibernate-matrix-databases";
	public static final String HIBERNATE_MATRIX_IGNORE = "hibernate-matrix-ignore";

	private static final String MATRIX_BUILD_FILE = "matrix.gradle";
	private static final String JDBC_DIR = "jdbc";

	private static final Logger log = Logging.getLogger( DatabaseProfilePlugin.class );

    private Project project;
	private List<DatabaseProfile> profiles;

    public void apply(Project project) {
        this.project = project;

		final LinkedHashMap<String, DatabaseProfile> profileMap = new LinkedHashMap<String, DatabaseProfile>();
		processStandardProfiles( profileMap );
		processCustomProfiles( profileMap );
		this.profiles = new ArrayList<DatabaseProfile>();

		DatabaseAllocationCleanUp listener = new DatabaseAllocationCleanUp();
		project.getGradle().addBuildListener( listener );
		for ( DatabaseProfile profile : profileMap.values() ) {
			this.profiles.add( profile );
			listener.addDatabaseAllocation( profile.getDatabaseAllocation() );
		}
    }

	private void processStandardProfiles(Map<String, DatabaseProfile> profileMap) {
		final File standardDatabasesDirectory = project.file( STANDARD_DATABASES_DIRECTORY );
		if ( standardDatabasesDirectory == null || ! standardDatabasesDirectory.exists() ) {
			log.debug( "Standard databases directory [{}] did not exist", STANDARD_DATABASES_DIRECTORY );
			return;
		}

		if ( ! standardDatabasesDirectory.isDirectory() ) {
			log.warn( "Located standard databases directory [{}] was not a directory", STANDARD_DATABASES_DIRECTORY );
			return;
		}

		processProfiles( standardDatabasesDirectory, profileMap );
	}

	private void processProfiles(File directory, Map<String, DatabaseProfile> profileMap) {
		// the directory itself is a "database directory" if it contains either:
		//		1) a file named 'matrix.gradle'
		//		2) a directory named 'jdbc'
		DatabaseProfile databaseProfile = null;
		final File matrixDotGradleFile = new File( directory, MATRIX_BUILD_FILE );
		if ( matrixDotGradleFile.exists() && matrixDotGradleFile.isFile() ) {
			log.debug( "Found matrix.gradle file : " + matrixDotGradleFile );
			databaseProfile = new MatrixDotGradleProfile( matrixDotGradleFile, project );
		}
		final File jdbcDirectory = new File( directory, JDBC_DIR );
		if ( jdbcDirectory.exists() && jdbcDirectory.isDirectory() ) {
			databaseProfile = new JdbcDirectoryProfile( jdbcDirectory, project );
		}

		if ( databaseProfile == null ) {
			// we determined this directory is not a database directory, check its sub-directories
			for ( File subDirectory : directory.listFiles() ) {
				if ( subDirectory.isDirectory() ) {
					processProfiles( subDirectory, profileMap );
				}
			}
			return; // EARLY EXIT!!!
		}

		final String profileName = databaseProfile.getName();
		if ( ignored().contains( profileName ) ) {
			log.debug( "Skipping ignored database profile [{}]", profileName );
			return;
		}

		DatabaseProfile previousEntry = profileMap.put( profileName, databaseProfile );
		if ( previousEntry != null ) {
			log.lifecycle(
					"Found duplicate profile definitions [name={}], [{}] taking precedence over [{}]",
					profileName,
					databaseProfile.getDirectory().getAbsolutePath(),
					previousEntry.getDirectory().getAbsolutePath()
			);
		}
	}

	private Set<String> ignored;

	private Set<String> ignored() {
		if ( ignored == null ) {
			final String values = System.getProperty( HIBERNATE_MATRIX_IGNORE );
			if ( values == null || values.length() == 0 ) {
				ignored = Collections.emptySet();
			}
			else {
				ignored = new HashSet<String>();
				Collections.addAll( ignored, values.split( "," ) );
			}
		}
		return ignored;
	}

	private void processCustomProfiles(Map<String, DatabaseProfile> profileMap) {
		final String customDatabaseDirectoryPath = System.getProperty( CUSTOM_DATABASES_DIRECTORY_KEY );
		if ( customDatabaseDirectoryPath != null && customDatabaseDirectoryPath.length() > 0 ) {
			final File customDatabaseDirectory = new File( customDatabaseDirectoryPath );
			if ( customDatabaseDirectory.exists() && customDatabaseDirectory.isDirectory() ) {
				processProfiles( customDatabaseDirectory, profileMap );
			}
		}
	}

	public Iterable<DatabaseProfile> getDatabaseProfiles() {
		return profiles;
	}
}
