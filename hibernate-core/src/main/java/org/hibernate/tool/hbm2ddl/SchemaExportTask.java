/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * An Ant task for <tt>SchemaExport</tt>.
 *
 * <pre>
 * &lt;taskdef name="schemaexport"
 *     classname="org.hibernate.tool.hbm2ddl.SchemaExportTask"
 *     classpathref="class.path"/&gt;
 *
 * &lt;schemaexport
 *     properties="${build.classes.dir}/hibernate.properties"
 *     quiet="no"
 *     text="no"
 *     drop="no"
 *     delimiter=";"
 *     output="${build.dir}/schema-export.sql"&gt;
 *     &lt;fileset dir="${build.classes.dir}"&gt;
 *         &lt;include name="*.hbm.xml"/&gt;
 *     &lt;/fileset&gt;
 * &lt;/schemaexport&gt;
 * </pre>
 *
 * @author Rong C Ou
 */
public class SchemaExportTask extends MatchingTask {
	private List<FileSet> fileSets = new LinkedList<FileSet>();
	private File propertiesFile;
	private File configurationFile;
	private File outputFile;
	private boolean quiet;
	private boolean text;
	private boolean drop;
	private boolean create;
	private boolean haltOnError;
	private String delimiter;
	private String implicitNamingStrategy;
	private String physicalNamingStrategy;

	@SuppressWarnings("UnusedDeclaration")
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
	 * Set a <literal>.cfg.xml</literal> file, which will be
	 * loaded as a resource, from the classpath
	 * @param configurationFile the path to the resource
	 */
	public void setConfig(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	/**
	 * Enable "quiet" mode. The schema will not be
	 * written to standard out.
	 * @param quiet true to enable quiet mode
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	/**
	 * Enable "text-only" mode. The schema will not
	 * be exported to the database.
	 * @param text true to enable text-only mode
	 */
	public void setText(boolean text) {
		this.text = text;
	}

	/**
	 * Enable "drop" mode. Database objects will be
	 * dropped but not recreated.
	 * @param drop true to enable drop mode
	 */
	public void setDrop(boolean drop) {
		this.drop = drop;
	}

	/**
	 * Enable "create" mode. Database objects will be
	 * created but not first dropped.
	 * @param create true to enable create mode
	 */
	public void setCreate(boolean create) {
		this.create = create;
	}

	/**
	 * Set the end of statement delimiter for the generated script
	 * @param delimiter the delimiter
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Set the script output file
	 * @param outputFile the file name
	 */
	public void setOutput(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * @deprecated Use {@link #setImplicitNamingStrategy} or {@link #setPhysicalNamingStrategy}
	 * instead
	 */
	@Deprecated
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
	public void setHaltonerror(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}

	/**
	 * Execute the task
	 */
	@Override
	public void execute() throws BuildException {
		try {
			doExecution();
		}
		catch (BuildException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BuildException( "Error performing export : " + e.getMessage(), e );
		}
	}

	private void doExecution() throws Exception {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		final MetadataSources metadataSources = new MetadataSources( bsr );

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

		ssrBuilder.applySetting( AvailableSettings.HBM2DDL_DELIMITER, delimiter );

		ExportType exportType = ExportType.interpret( drop, create );
		Target output = Target.interpret( !quiet, !text );

		if ( output.doScript() ) {
			ssrBuilder.applySetting( AvailableSettings.HBM2DDL_SCRIPTS_ACTION, exportType.getAction() );

			final Object scriptTarget;
			if ( outputFile == null ) {
				scriptTarget = new OutputStreamWriter( System.out );
			}
			else {
				scriptTarget = outputFile;
			}

			if ( exportType.doCreate() ) {
				ssrBuilder.applySetting( AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET, scriptTarget );
			}
			if ( exportType.doDrop() ) {
				ssrBuilder.applySetting( AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET, scriptTarget );
			}
		}

		if ( output.doExport() ) {
			ssrBuilder.applySetting( AvailableSettings.HBM2DDL_DATABASE_ACTION, exportType.getAction() );
		}


		final StandardServiceRegistryImpl ssr = (StandardServiceRegistryImpl) ssrBuilder.build();


		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder( ssr );

		ClassLoaderService classLoaderService = bsr.getService( ClassLoaderService.class );
		if ( implicitNamingStrategy != null ) {
			metadataBuilder.applyImplicitNamingStrategy(
					(ImplicitNamingStrategy) classLoaderService.classForName( implicitNamingStrategy ).newInstance()
			);
		}
		if ( physicalNamingStrategy != null ) {
			metadataBuilder.applyPhysicalNamingStrategy(
					(PhysicalNamingStrategy) classLoaderService.classForName( physicalNamingStrategy ).newInstance()
			);
		}

		final MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();
		metadata.validate();

		SchemaManagementToolCoordinator.process(
				metadata,
				ssr,
				ssr.getService( ConfigurationService.class ).getSettings(),
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}

	private String[] getFiles() {
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

		return ArrayHelper.toStringArray(files);
	}

	public enum ExportType {
		CREATE( Action.CREATE_ONLY ),
		DROP( Action.DROP ),
		NONE( Action.NONE ),
		BOTH( Action.CREATE );

		private final Action action;

		ExportType(Action action) {
			this.action = action;
		}

		public boolean doCreate() {
			return this == BOTH || this == CREATE;
		}

		public boolean doDrop() {
			return this == BOTH || this == DROP;
		}

		public Action getAction() {
			return action;
		}

		public static ExportType interpret(boolean justDrop, boolean justCreate) {
			if ( justDrop ) {
				return ExportType.DROP;
			}
			else if ( justCreate ) {
				return ExportType.CREATE;
			}
			else {
				return ExportType.BOTH;
			}
		}
	}

}
