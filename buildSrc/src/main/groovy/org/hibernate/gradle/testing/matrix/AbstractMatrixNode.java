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

package org.hibernate.gradle.testing.matrix;

import java.io.File;
import java.util.Map;

import hudson.util.DBAllocation;
import hudson.util.DBAllocationHelper;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import org.hibernate.gradle.util.FileUtil;

/**
 * @author Strong Liu
 */
public abstract class AbstractMatrixNode implements MatrixNode {
    private static final Logger log = Logging.getLogger( AbstractMatrixNode.class );
    private final String name;
    private final File baseOutputDirectory;
    private final DBAllocation dbAllocation;
    private File propertyFile;
    private Map<String, String> properties;
    private String uuid;

    protected AbstractMatrixNode(final Project project, final String name) {
        this.name = name;
        this.baseOutputDirectory = new File( new File( project.getBuildDir(), "matrix" ), getName() );
        FileUtil.mkdir( baseOutputDirectory );
        this.dbAllocation = new DBAllocation( baseOutputDirectory );
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public File getBaseOutputDirectory() {
        return baseOutputDirectory;
    }

    @Override
    public Configuration getTestingRuntimeConfiguration() {
        return getDependencyResolver().resolve();
    }

    @Override
    public void release() {
        if ( DBAllocationHelper.isDBAllocationEnabled( getName() ) ) {
            if ( uuid != null ) {
                try {
                    dbAllocation.release( uuid );
                }
                catch ( RuntimeException e ) {
                    log.warn( "DBAllocator failed to release db["+getName()+"]", e );
                }
            } else {
                log.warn( getName() + "is enabled to use DBAllocation, but the allocated uuid is null" );
            }

        }
    }

    @Override
    public DBAllocation getDBAllocation() {
        return dbAllocation;
    }

    @Override
    public void setHibernatePropertyFile(File file) {
        this.propertyFile = file;
    }

    @Override
    public File getHibernatePropertyFile() {
        return propertyFile;
    }

    @Override
    public Map<String, String> getProperties() {
        if ( properties == null ) {
            properties = DBAllocationHelper.getProperties( this );
            uuid = properties.get( "uuid" );
        }
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        AbstractMatrixNode that = (AbstractMatrixNode) o;

        if ( name != null ? !name.equals( that.name ) : that.name != null ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
