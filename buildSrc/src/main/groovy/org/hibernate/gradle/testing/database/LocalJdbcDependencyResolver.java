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

package org.hibernate.gradle.testing.database;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import org.hibernate.gradle.util.FileUtil;
import org.hibernate.gradle.util.IvyResolutionHelper;

/**
 * @author Strong Liu
 */
@SuppressWarnings("cast")
public class LocalJdbcDependencyResolver implements DependencyResolver {
    private static final Logger log = Logging.getLogger( LocalJdbcDependencyResolver.class );

    private final File databaseDir;
    private final Project project;
    private final IvyResolutionHelper ivyResolutionHelper;
    private Configuration configuration;

    public LocalJdbcDependencyResolver(Project project, File databaseDir) {
        this.databaseDir = databaseDir;
        this.project = project;
        this.ivyResolutionHelper = new IvyResolutionHelper( project );
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Configuration resolve() {
        if ( configuration == null ) {
            final File jdbcDir = new File( databaseDir, "jdbc" );
            if ( FileUtil.isDirectory( jdbcDir ) ) {
                configuration = createSelfContainedConfiguration( jdbcDir, databaseDir.getName() );
            }
            else {
                log.warn( "Found 'jdbc' directory, but no entries" );
            }
            if ( configuration != null ) {
                configuration.setVisible( true );
                configuration.setDescription( "The [" + databaseDir.getName() + "] JDBC dependency configuration" );

            }
        }
        return configuration;
    }

    private Configuration createSelfContainedConfiguration(File directory, String configurationName) {
        Configuration configuration = ivyResolutionHelper.getOrCreateConfiguration( configurationName );
        DefaultSelfResolvingDependency dependency =
                new DefaultSelfResolvingDependency( project.files( (File[]) directory.listFiles() ) );
        configuration.addDependency( dependency );
        return configuration;
    }

}
