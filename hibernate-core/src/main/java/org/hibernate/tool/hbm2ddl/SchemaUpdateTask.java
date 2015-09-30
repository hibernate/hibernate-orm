/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * An Ant task for <tt>SchemaUpdate</tt>.
 * <p/>
 * <pre>
 * &lt;taskdef name="schemaupdate"
 *     classname="org.hibernate.tool.hbm2ddl.SchemaUpdateTask"
 *     classpathref="class.path"/&gt;
 *
 * &lt;schemaupdate
 *     properties="${build.classes.dir}/hibernate.properties"
 *     quiet="no"
 *     &lt;fileset dir="${build.classes.dir}"&gt;
 *         &lt;include name="*.hbm.xml"/&gt;
 *     &lt;/fileset&gt;
 * &lt;/schemaupdate&gt;
 * </pre>
 *
 * @author Rong C Ou, Gavin King
 * @see SchemaUpdate
 */
public class SchemaUpdateTask extends MatchingTask {
	private List<FileSet> fileSets = new LinkedList<FileSet>();
	private File propertiesFile;
	private File configurationFile;
	private File outputFile;
	private boolean quiet;
	private boolean text = true;
	private boolean haltOnError;
	private String delimiter;

	private String implicitNamingStrategy = null;
	private String physicalNamingStrategy = null;

	@SuppressWarnings("UnusedDeclaration")
	public void addFileset(FileSet fileSet) {
		fileSets.add( fileSet );
	}

	/**
	 * Set a properties file
	 *
	 * @param propertiesFile the properties file name
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setProperties(File propertiesFile) {
		if ( !propertiesFile.exists() ) {
			throw new BuildException( "Properties file: " + propertiesFile + " does not exist." );
		}

		log( "Using properties file " + propertiesFile, Project.MSG_DEBUG );
		this.propertiesFile = propertiesFile;
	}

	/**
	 * Set a {@code cfg.xml} file
	 *
	 * @param configurationFile the file name
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setConfig(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	/**
	 * Enable "text-only" mode. The schema will not be updated in the database.
	 *
	 * @param text true to enable text-only mode
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setText(boolean text) {
		this.text = text;
	}

	/**
	 * Enable "quiet" mode. The schema will not be written to standard out.
	 *
	 * @param quiet true to enable quiet mode
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setNamingStrategy(String namingStrategy) {
		DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyAntArgument();
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setImplicitNamingStrategy(String implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setPhysicalNamingStrategy(String physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
	}

	@SuppressWarnings("UnusedDeclaration")
	public File getOutputFile() {
		return outputFile;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isHaltOnError() {
		return haltOnError;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setHaltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getDelimiter() {
		return delimiter;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Execute the task
	 */
	@Override
	public void execute() throws BuildException {
		log( "Running Hibernate Core SchemaUpdate." );
		log( "This is an Ant task supporting only mapping files, if you want to use annotations see http://tools.hibernate.org." );

		try {
			final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder();
			configure( ssrBuilder );

			final StandardServiceRegistry ssr = ssrBuilder.build();

			final MetadataSources metadataSources = new MetadataSources( ssr );
			configure( metadataSources );

			final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
			configure( metadataBuilder, ssr );

			final MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();

			final SchemaUpdate su = new SchemaUpdate( metadata );
			su.setOutputFile( outputFile.getPath() );
			su.setDelimiter( delimiter );
			su.setHaltOnError( haltOnError );
			su.execute( !quiet, !text );
		}
		catch (HibernateException e) {
			throw new BuildException( "Schema text failed: " + e.getMessage(), e );
		}
		catch (FileNotFoundException e) {
			throw new BuildException( "File not found: " + e.getMessage(), e );
		}
		catch (IOException e) {
			throw new BuildException( "IOException : " + e.getMessage(), e );
		}
		catch (BuildException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BuildException( e );
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
			properties.load( new FileInputStream( propertiesFile ) );
		}

		registryBuilder.applySettings( properties );
	}

	private void configure(MetadataSources metadataSources) {
		for ( String filename : collectFiles() ) {
			if ( filename.endsWith( ".jar" ) ) {
				metadataSources.addJar( new File( filename ) );
			}
			else {
				metadataSources.addFile( filename );
			}
		}
	}

	private String[] collectFiles() {
		List<String> files = new LinkedList<String>();
		for ( FileSet fileSet : fileSets ) {
			final DirectoryScanner ds = fileSet.getDirectoryScanner( getProject() );
			final String[] dsFiles = ds.getIncludedFiles();
			for ( String dsFileName : dsFiles ) {
				File f = new File( dsFileName );
				if ( !f.isFile() ) {
					f = new File( ds.getBasedir(), dsFileName );
				}

				files.add( f.getAbsolutePath() );
			}
		}
		return ArrayHelper.toStringArray( files );
	}

	@SuppressWarnings("deprecation")
	private void configure(MetadataBuilder metadataBuilder, StandardServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		if ( implicitNamingStrategy != null ) {
			try {
				metadataBuilder.applyImplicitNamingStrategy(
						(ImplicitNamingStrategy) classLoaderService.classForName( implicitNamingStrategy ).newInstance()
				);
			}
			catch (Exception e) {
				throw new BuildException(
						"Unable to instantiate specified ImplicitNamingStrategy [" + implicitNamingStrategy + "]",
						e
				);
			}
		}

		if ( physicalNamingStrategy != null ) {
			try {
				metadataBuilder.applyPhysicalNamingStrategy(
						(PhysicalNamingStrategy) classLoaderService.classForName( physicalNamingStrategy ).newInstance()
				);
			}
			catch (Exception e) {
				throw new BuildException(
						"Unable to instantiate specified PhysicalNamingStrategy [" + physicalNamingStrategy + "]",
						e
				);
			}
		}
	}

}
