/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformationBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * An Ant task for <tt>SchemaUpdate</tt>.
 *
 * <pre>
 * &lt;taskdef name="schemavalidator"
 *     classname="org.hibernate.tool.hbm2ddl.SchemaValidatorTask"
 *     classpathref="class.path"/&gt;
 *
 * &lt;schemaupdate
 *     properties="${build.classes.dir}/hibernate.properties"
 *     &lt;fileset dir="${build.classes.dir}"&gt;
 *         &lt;include name="*.hbm.xml"/&gt;
 *     &lt;/fileset&gt;
 * &lt;/schemaupdate&gt;
 * </pre>
 *
 * @author Gavin King
 */
public class SchemaValidatorTask extends MatchingTask {

	private List fileSets = new LinkedList();
	private File propertiesFile;
	private File configurationFile;
	private String namingStrategy;

	public void addFileset(FileSet set) {
		fileSets.add(set);
	}

	/**
	 * Set a properties file
	 * @param propertiesFile the properties file name
	 */
	public void setProperties(File propertiesFile) {
		if ( !propertiesFile.exists() ) {
			throw new BuildException("Properties file: " + propertiesFile + " does not exist.");
		}

		log("Using properties file " + propertiesFile, Project.MSG_DEBUG);
		this.propertiesFile = propertiesFile;
	}

	/**
	 * Set a <literal>.cfg.xml</literal> file
	 * @param configurationFile the file name
	 */
	public void setConfig(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	/**
	 * Execute the task
	 */
	@Override
    public void execute() throws BuildException {
		try {
			doIt();
		}
		catch (HibernateException e) {
			throw new BuildException("Schema text failed: " + e.getMessage(), e);
		}
		catch (FileNotFoundException e) {
			throw new BuildException("File not found: " + e.getMessage(), e);
		}
		catch (IOException e) {
			throw new BuildException("IOException : " + e.getMessage(), e);
		}
		catch (Exception e) {
			throw new BuildException(e);
		}
	}

	private void doIt() throws Exception {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

		final MetadataSources metadataSources = new MetadataSources( bsr );
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		if ( configurationFile != null ) {
			ssrBuilder.configure( configurationFile );
		}
		if ( propertiesFile != null ) {
			ssrBuilder.loadProperties( propertiesFile );
		}
		ssrBuilder.applySettings( getProject().getProperties() );

		for ( String fileName : getFiles() ) {
			if ( fileName.endsWith(".jar") ) {
				metadataSources.addJar( new File( fileName ) );
			}
			else {
				metadataSources.addFile( fileName );
			}
		}

		final StandardServiceRegistryImpl ssr = (StandardServiceRegistryImpl) ssrBuilder.build();

		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder( ssr );
		if ( namingStrategy != null ) {
			final ClassLoaderService classLoaderService = bsr.getService( ClassLoaderService.class );
			final NamingStrategy namingStrategyInstance = (NamingStrategy) classLoaderService.classForName( namingStrategy ).newInstance();
			metadataBuilder.with( namingStrategyInstance );
		}
		final MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();

		final JdbcEnvironment jdbcEnvironment = ssr.getService( JdbcEnvironment.class );
		final ConnectionProvider connectionProvider = ssr.getService( ConnectionProvider.class );
		final JdbcConnectionAccess connectionAccess = new ConnectionProviderJdbcConnectionAccess( connectionProvider );
		final DatabaseInformation existing = new DatabaseInformationBuilder( jdbcEnvironment, connectionAccess ).build();

		final org.hibernate.tool.schema.spi.Target target = new org.hibernate.tool.schema.spi.Target() {
			private Statement stmnt;

			@Override
			public boolean acceptsImportScriptActions() {
				return false;
			}

			@Override
			public void prepare() {
				try {
					stmnt = connectionAccess.obtainConnection().createStatement();
				}
				catch (SQLException e) {
					throw new HibernateException( "Could not build JDBC Statement", e );
				}
			}

			@Override
			public void accept(String action) {
				try {
					stmnt.execute( action );
				}
				catch (SQLException e) {
					throw new HibernateException( "Could not execute command via JDBC", e );
				}
			}

			@Override
			public void release() {
				try {
					stmnt.close();
				}
				catch (SQLException e) {
					throw new HibernateException( "Could not release JDBC Statement", e );
				}
			}
		};

		final org.hibernate.tool.schema.spi.SchemaValidator validator = ssr.getService( SchemaManagementTool.class )
				.getSchemaValidator( Collections.emptyMap() );

		validator.doValidation( metadata.getDatabase(), existing );
	}

	private static class ConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final ConnectionProvider connectionProvider;

		public ConnectionProviderJdbcConnectionAccess(ConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getConnection();
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.closeConnection( connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	private String[] getFiles() {

		List files = new LinkedList();
		for ( Iterator i = fileSets.iterator(); i.hasNext(); ) {

			FileSet fs = (FileSet) i.next();
			DirectoryScanner ds = fs.getDirectoryScanner( getProject() );

			String[] dsFiles = ds.getIncludedFiles();
			for (int j = 0; j < dsFiles.length; j++) {
				File f = new File(dsFiles[j]);
				if ( !f.isFile() ) {
					f = new File( ds.getBasedir(), dsFiles[j] );
				}

				files.add( f.getAbsolutePath() );
			}
		}

		return ArrayHelper.toStringArray( files );
	}

	public void setNamingStrategy(String namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

}
