/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logging;
import org.slf4j.Logger;

import org.hibernate.build.qalab.DatabaseAllocation;
import org.hibernate.build.qalab.DatabaseAllocator;

/**
 * Basic support for {@link DatabaseProfile} implementations
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public abstract class AbstractDatabaseProfileImpl implements DatabaseProfile {
	private static final Logger log = Logging.getLogger( AbstractDatabaseProfileImpl.class );

    private final String name;
	private final File profileDirectory;
	private final Project project;
	private final Map<String,String> hibernateProperties;
	private final DatabaseAllocation databaseAllocation;

	@SuppressWarnings( {"unchecked"})
	protected AbstractDatabaseProfileImpl(File profileDirectory, Project project) {
		this.profileDirectory = profileDirectory;
		this.name = profileDirectory.getName();
		this.project = project;

		this.hibernateProperties = new HashMap<String, String>();
		final File hibernatePropertiesFile = new File(
				new File( profileDirectory, "resources" ),
				"hibernate.properties"
		);
		if ( hibernatePropertiesFile.exists() ) {
			Properties props = new Properties();
			try {
				FileInputStream stream = new FileInputStream( hibernatePropertiesFile );
				try {
					props.load( stream );
				}
				finally {
					try {
						stream.close();
					}
					catch (IOException ignore) {
					}
				}
			}
			catch (IOException e) {
				log.warn( "Unable to read Hibernate properties for database profile [" + name + "]", e );
			}
			for ( String propName : props.stringPropertyNames() ) {
				hibernateProperties.put( propName, props.getProperty( propName ) );
			}
		}

		this.databaseAllocation = DatabaseAllocator.locate( project ).getAllocation( this );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public File getDirectory() {
		return profileDirectory;
	}

	@Override
	public Map<String, String> getHibernateProperties() {
		return hibernateProperties;
	}

	@Override
	public DatabaseAllocation getDatabaseAllocation() {
		return databaseAllocation;
	}

	protected Configuration prepareConfiguration(String name) {
        Configuration configuration = getOrCreateConfiguration( name );
        configuration.setDescription( "The JDBC dependency configuration for the [" + name + "] profile" );
        return configuration;
    }

    protected Configuration getOrCreateConfiguration(String name) {
        Configuration configuration = project.getConfigurations().findByName( name );
        if ( configuration == null ) {
            configuration = project.getConfigurations().add( name );
        }
        return configuration;
    }
}
