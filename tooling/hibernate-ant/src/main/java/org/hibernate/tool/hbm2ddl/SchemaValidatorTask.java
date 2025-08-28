/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * An Ant task for {@code SchemaUpdate}.
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
 * @see SchemaValidator
 * @author Gavin King
 */
public class SchemaValidatorTask extends MatchingTask {
	private final List<FileSet> fileSets = new LinkedList<>();

	private File propertiesFile;
	private File configurationFile;

	private String implicitNamingStrategy = null;
	private String physicalNamingStrategy = null;


	public void addFileset(FileSet fileSet) {
		fileSets.add( fileSet );
	}

	/**
	 * Set a properties file
	 * @param propertiesFile the properties file name
	 */
	public void setProperties(File propertiesFile) {
		if ( !propertiesFile.exists() ) {
			throw new BuildException("Properties file [" + propertiesFile + "] does not exist.");
		}

		log( "Using properties file " + propertiesFile, Project.MSG_DEBUG );
		this.propertiesFile = propertiesFile;
	}

	/**
	 * Set a {@code .cfg.xml} file
	 * @param configurationFile the file name
	 */
	public void setConfig(File configurationFile) {
		if ( !configurationFile.exists() ) {
			throw new BuildException("Configuration file [" + configurationFile + "] does not exist.");
		}

		log( "Using configuration file " + propertiesFile, Project.MSG_DEBUG );
		this.configurationFile = configurationFile;
	}

	public void setNamingStrategy(String namingStrategy) {
		DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyAntArgument();
	}

	public void setImplicitNamingStrategy(String implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
	}

	public void setPhysicalNamingStrategy(String physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
	}

	/**
	 * Execute the task
	 */
	@Override
	public void execute() throws BuildException {
		try {
			final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder();
			configure( ssrBuilder );

			final StandardServiceRegistry ssr = ssrBuilder.build();

			try {
				final MetadataSources metadataSources = new MetadataSources( ssrBuilder.build() );
				configure( metadataSources );

				final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
				configure( metadataBuilder, ssr );

				final MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();

				new SchemaValidator().validate( metadata, ssr );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( ssr );
			}
		}
		catch (HibernateException e) {
			throw new BuildException("Schema text failed: " + e.getMessage(), e);
		}
		catch (FileNotFoundException e) {
			throw new BuildException("File not found: " + e.getMessage(), e);
		}
		catch (IOException e) {
			throw new BuildException("IOException: " + e.getMessage(), e);
		}
		catch (BuildException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BuildException(e);
		}
	}
	private void configure(StandardServiceRegistryBuilder registryBuilder) throws IOException {
		if ( configurationFile != null ) {
			registryBuilder.configure( configurationFile );
		}

		Properties properties = new Properties();
		if ( propertiesFile == null ) {
			properties.putAll( getProject().getProperties() );
		}
		else {
			try ( final FileInputStream fis = new FileInputStream( propertiesFile ) ) {
				properties.load( fis );
			}
		}

		registryBuilder.applySettings( properties );
	}

	private void configure(MetadataSources metadataSources) {
		for ( String filename : collectFiles() ) {
			if ( filename.endsWith(".jar") ) {
				metadataSources.addJar( new File( filename ) );
			}
			else {
				metadataSources.addFile( filename );
			}
		}
	}

	private String[] collectFiles() {
		List<String> files = new ArrayList<>();

		for ( FileSet fileSet : fileSets ) {
			final DirectoryScanner ds = fileSet.getDirectoryScanner( getProject() );

			for ( String dsFile : ds.getIncludedFiles() ) {
				File f = new File( dsFile );
				if ( !f.isFile() ) {
					f = new File( ds.getBasedir(), dsFile );
				}
				files.add( f.getAbsolutePath() );
			}
		}

		return ArrayHelper.toStringArray( files );
	}

	private void configure(MetadataBuilder metadataBuilder, StandardServiceRegistry serviceRegistry) {
		final StrategySelector strategySelector = serviceRegistry.requireService( StrategySelector.class );
		if ( implicitNamingStrategy != null ) {
			metadataBuilder.applyImplicitNamingStrategy(
					strategySelector.resolveStrategy( ImplicitNamingStrategy.class, implicitNamingStrategy )
			);
		}
		if ( physicalNamingStrategy != null ) {
			metadataBuilder.applyPhysicalNamingStrategy(
					strategySelector.resolveStrategy( PhysicalNamingStrategy.class, physicalNamingStrategy )
			);
		}
	}
}
