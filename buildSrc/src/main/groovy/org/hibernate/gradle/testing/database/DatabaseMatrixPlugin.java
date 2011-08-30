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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.Closure;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import org.hibernate.gradle.testing.matrix.AbstractMatrixNode;
import org.hibernate.gradle.testing.matrix.MatrixNode;
import org.hibernate.gradle.testing.matrix.MatrixNodeProvider;
import org.hibernate.gradle.util.DuplicatedDBConfigException;
import org.hibernate.gradle.util.FileUtil;
import org.hibernate.gradle.util.Jdk;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class DatabaseMatrixPlugin implements Plugin<Project>, MatrixNodeProvider {
    private static final Logger log = Logging.getLogger( DatabaseMatrixPlugin.class );
    public static final String DEFAULT_DATABASE_DIRECTORY = "databases";
    private Project project;
    private Map<String, MatrixNode> matrixNodeMap = new HashMap();

    public void apply(Project project) {
        this.project = project;
        applyDatabaseDirectories( getRootProject( project ).file( DEFAULT_DATABASE_DIRECTORY ) );
        String databasesDirPath = System.getProperty( "hibernate-matrix-databases" );
        if(databasesDirPath!=null){
            File databaseDir = new File( databasesDirPath );
            if(FileUtil.isDirectory( databaseDir )){
                applyDatabaseDirectories( databaseDir );
            }
        }
    }

    private Project getRootProject(Project project) {
        return project.getParent() != null ? getRootProject( project.getParent() ) : project;
    }


    private void applyDatabaseDirectories(Object databasesBaseDirObject) {
        if ( databasesBaseDirObject == null ) {
            return;
        }
        log.debug( "Applying database directory: " + databasesBaseDirObject );
        final File databasesBaseDir = project.file( databasesBaseDirObject );
        for ( File entry : databasesBaseDir.listFiles() ) {
            if ( entry.isDirectory() ) {
                applyPossibleDatabaseDirectory( entry );
            }
        }
    }

    private static final String MATRIX_NODE_CONVENTION_KEY = "matrixNode";
    private static final String MATRIX_BUILD_FILE = "matrix.gradle";
//    private static final String IVY_XML_FILE = "ivy.xml";

    private boolean ignoreDefault(String databaseName) {
        String value = System.getProperty( "hibernate-matrix-ignore" );
        return ( value != null && ( value.equals( "all" ) || value.contains( databaseName ) ) );
    }

    private void applyPossibleDatabaseDirectory(final File databaseDir) {
        final String databaseName = databaseDir.getName();
        if ( ignoreDefault( databaseName ) ) {
            return;
        }
        log.debug( "Checking potential database directory : {}", databaseName );

        MatrixNode node = null;

        // 3 types of support here:
        //		1) directory contains a file named 'matrix.gradle'
        // 		2) directory contains a file named 'ivy.xml'   (we prefer matrix.gradle --stliu)
        //		3) directory contains a sub-directory named 'jdbc' containing the driver artifacts

        final File matrixFile = new File( databaseDir, MATRIX_BUILD_FILE );
        if ( FileUtil.isFile( matrixFile ) ) {

            // (1) we found the 'matrix.gradle' file
            node = prepareFromGradleFile( matrixFile );
        }
        else {
//            final File ivyXmlFile = new File( databaseDir, IVY_XML_FILE );
//            if ( FileUtil.isFile( ivyXmlFile ) ) {
//                // (2) we found the 'ivy.xml' file
//                node = prepareFromIvyXmlFile( ivyXmlFile );
//            }
//            else {
                final File jdbcDir = new File( databaseDir, "jdbc" );
                if ( FileUtil.isDirectory( jdbcDir ) ) {
                    node = prepareFromJdbcDir( jdbcDir );
                }
//            }
        }

        if ( node == null ) {
            log.info( "Doesn't found valid Matrix database configuration file in directory : {}", databaseDir );
            return;
        }

        final File propertiesFile = new File( new File( databaseDir, "resources" ), "hibernate.properties" );
        if ( FileUtil.isFile( propertiesFile ) ) {
            node.setHibernatePropertyFile( propertiesFile );
        }
        else {
            log.warn( "No 'hibernate.properties' found in {}/resources", databaseDir );
        }
        log.debug( "Adding node[{}] " + node.getName() );
        if ( matrixNodeMap.containsKey( node.getName() ) ) {
            throw new DuplicatedDBConfigException( "There is already a Matrix node named " + node.getName() );
        }
        else {
            matrixNodeMap.put( node.getName(), node );
        }
    }

    private MatrixNode prepareFromGradleFile(File matrixFile) {
        log.debug( "Found matrix file : " + matrixFile );
        MatrixDotGradleMatrixNodeImpl matrixNode = new MatrixDotGradleMatrixNodeImpl(
                matrixFile.getParentFile()
                        .getName()
        );
        MatrixDotGradleMatrixNodeConvention convention = new MatrixDotGradleMatrixNodeConvention( matrixNode );
        project.getConvention().getPlugins().put( MATRIX_NODE_CONVENTION_KEY, convention );
        try {
            project.apply( Collections.singletonMap( "from", matrixFile ) );
        }
        finally {
            project.getConvention().getPlugins().remove( MATRIX_NODE_CONVENTION_KEY );
        }
        return matrixNode;
    }

    /**
     * {@link MatrixNode} implementation for handling 'matrix.gradle' files
     */
    private class MatrixDotGradleMatrixNodeImpl extends AbstractMatrixNode {
        private final Configuration jdbcDependencies;
        private Jdk jdk;

        public MatrixDotGradleMatrixNodeImpl(String name) {
            super( project, name );
            this.jdbcDependencies = prepareConfiguration( name );
            this.jdk = getDefaultJdk();
        }

        public Jdk getTestingRuntimeJdk() {
            return jdk;
        }

        @Override
        public DependencyResolver getDependencyResolver() {
            return new DependencyResolver() {
                @Override
                public Project getProject() {
                    return project;
                }

                @Override
                public Configuration resolve() {
                    return jdbcDependencies;
                }
            };
        }
    }

    /**
     * Provides simplified convention object to the database-specific script for convenient configuration.
     */
    private class MatrixDotGradleMatrixNodeConvention {
        private final MatrixDotGradleMatrixNodeImpl matrixNode;

        private MatrixDotGradleMatrixNodeConvention(MatrixDotGradleMatrixNodeImpl matrixNode) {
            this.matrixNode = matrixNode;
        }

        public void jdbcDependency(Object dependencyNotation, Closure closure) {
            project.getDependencies().add( matrixNode.jdbcDependencies.getName(), dependencyNotation, closure );
        }

        public void jdbcDependency(Object dependencyNotation) {
            log.debug(
                    "Adding JDBC dependency[{}] resolved from matrix.gradle",
                    matrixNode.jdbcDependencies.getName()
            );
            project.getDependencies().add( matrixNode.jdbcDependencies.getName(), dependencyNotation );
        }

        public void jdk(Jdk jdk) {
            matrixNode.jdk = jdk;
        }
    }

    private MatrixNode prepareFromJdbcDir(File jdbcDir) {
        log.debug( "Found local jdbc dir : " + jdbcDir );
        return new LocalMatrixNode( jdbcDir );
    }

    private class LocalMatrixNode extends AbstractMatrixNode {
        private final Jdk jdk = getDefaultJdk();
        private final LocalJdbcDependencyResolver resolver;

        private LocalMatrixNode(File jdbcDir) {
            super( project, jdbcDir.getParentFile().getName() );
            resolver = new LocalJdbcDependencyResolver( project, jdbcDir.getParentFile() );
        }

        public Jdk getTestingRuntimeJdk() {
            return jdk;
        }

        @Override
        public DependencyResolver getDependencyResolver() {
            return resolver;
        }
    }

    public List<MatrixNode> getMatrixNodes() {
        return Collections.unmodifiableList( new ArrayList<MatrixNode>( matrixNodeMap.values() ) );
    }

    private Configuration prepareConfiguration(String name) {
        Configuration configuration = getOrCreateConfiguration( name );
        configuration.setDescription( "The [" + name + "] JDBC dependency configuration" );
        return configuration;
    }

    private Configuration getOrCreateConfiguration(String configurationName) {
        Configuration configuration = project.getConfigurations().findByName( configurationName );
        if ( configuration == null ) {
            configuration = project.getConfigurations().add( configurationName );
        }
        return configuration;
    }

    private Jdk getDefaultJdk() {
        return new Jdk();
    }
}
