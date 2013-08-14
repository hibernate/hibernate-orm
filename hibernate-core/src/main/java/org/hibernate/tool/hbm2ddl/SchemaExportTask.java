/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;

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
 * @see SchemaExport
 * @author Rong C Ou
 */
public class SchemaExportTask extends MatchingTask {

	private List fileSets = new LinkedList();
	private File propertiesFile = null;
	private File configurationFile = null;
	private File outputFile = null;
	private boolean quiet = false;
	private boolean text = false;
	private boolean drop = false;
	private boolean create = false;
	private boolean haltOnError = false;
	private String delimiter = null;
	private String namingStrategy = null;

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
	 * Execute the task
	 */
	@Override
    public void execute() throws BuildException {
		try {
			getSchemaExport( getConfiguration() ).execute(!quiet, !text, drop, create);
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

		return ArrayHelper.toStringArray(files);
	}

	private Configuration getConfiguration() throws Exception {
		Configuration cfg = new Configuration();
		if (namingStrategy!=null) {
			cfg.setNamingStrategy(
					(NamingStrategy) ReflectHelper.classForName(namingStrategy).newInstance()
				);
		}
		if (configurationFile != null) {
			cfg.configure( configurationFile );
		}

		String[] files = getFiles();
		for (int i = 0; i < files.length; i++) {
			String filename = files[i];
			if ( filename.endsWith(".jar") ) {
				cfg.addJar( new File(filename) );
			}
			else {
				cfg.addFile(filename);
			}
		}
		return cfg;
	}

	private SchemaExport getSchemaExport(Configuration cfg) throws HibernateException, IOException {
		Properties properties = new Properties();
		properties.putAll( cfg.getProperties() );
		if (propertiesFile == null) {
			properties.putAll( getProject().getProperties() );
		}
		else {
			properties.load( new FileInputStream(propertiesFile) );
		}
		cfg.setProperties(properties);
		return new SchemaExport(cfg)
				.setHaltOnError(haltOnError)
				.setOutputFile( outputFile.getPath() )
				.setDelimiter(delimiter);
	}

	public void setNamingStrategy(String namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public void setHaltonerror(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}

}
