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
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import org.hibernate.gradle.util.FileUtil;
import org.hibernate.gradle.util.IvyResolutionHelper;
import org.hibernate.gradle.util.ResolutionException;

/**
 * @author Strong Liu
 */
public class IvyDependencyResolver implements DependencyResolver {
    private static final Logger log = Logging.getLogger( IvyDependencyResolver.class );

    private final File ivyXml;
    private final Project project;
    private final IvyResolutionHelper ivyResolutionHelper;
    private Configuration configuration;

    public IvyDependencyResolver(File ivyXml, Project project) {
        this.ivyXml = ivyXml;
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
            if ( FileUtil.isFile( ivyXml ) ) {
                String databaseName = ivyXml.getParentFile().getName();
                try {
                    log.debug(
                            "Using IvyDependencyResolver to resolve {} dependencies with file {} ",
                            databaseName,
                            ivyXml
                    );
                    configuration = ivyResolutionHelper.resolve( ivyXml, databaseName );
                }
                catch ( ResolutionException e ) {
                    log.warn( "Skipping database '{}' due to problems resolving dependencies", databaseName );
                }

                if ( configuration != null ) {
                    configuration.setVisible( true );
                    configuration.setDescription(
                            "The [" + ivyXml.getParentFile()
                                    .getName() + "] JDBC dependency configuration"
                    );

                }
            }
        }
        return configuration;
    }
}
