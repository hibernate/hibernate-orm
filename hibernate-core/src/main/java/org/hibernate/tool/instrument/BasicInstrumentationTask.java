/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.instrument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.bytecode.buildtime.spi.Instrumenter;
import org.hibernate.bytecode.buildtime.spi.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Super class for all Hibernate instrumentation tasks.  Provides the basic templating of how instrumentation
 * should occur; subclasses simply plug in to that process appropriately for the given bytecode provider.
 *
 * @author Steve Ebersole
 */
public abstract class BasicInstrumentationTask extends Task implements Instrumenter.Options {

	private final LoggerBridge logger = new LoggerBridge();

	private List filesets = new ArrayList();
	private boolean extended;

	// deprecated option...
	private boolean verbose;

	public void addFileset(FileSet set) {
		this.filesets.add( set );
	}

	protected final Iterator filesets() {
		return filesets.iterator();
	}

	public boolean isExtended() {
		return extended;
	}

	public void setExtended(boolean extended) {
		this.extended = extended;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public final boolean performExtendedInstrumentation() {
		return isExtended();
	}

	protected abstract Instrumenter buildInstrumenter(Logger logger, Instrumenter.Options options);

	@Override
    public void execute() throws BuildException {
		try {
			buildInstrumenter( logger, this )
					.execute( collectSpecifiedFiles() );
		}
		catch ( Throwable t ) {
			throw new BuildException( t );
		}
	}

	private Set collectSpecifiedFiles() {
		HashSet files = new HashSet();
		Project project = getProject();
		Iterator filesets = filesets();
		while ( filesets.hasNext() ) {
			FileSet fs = ( FileSet ) filesets.next();
			DirectoryScanner ds = fs.getDirectoryScanner( project );
			String[] includedFiles = ds.getIncludedFiles();
			File d = fs.getDir( project );
			for ( int i = 0; i < includedFiles.length; ++i ) {
				files.add( new File( d, includedFiles[i] ) );
			}
		}
		return files;
	}

	protected class LoggerBridge implements Logger {
		public void trace(String message) {
			log( message, Project.MSG_VERBOSE );
		}

		public void debug(String message) {
			log( message, Project.MSG_DEBUG );
		}

		public void info(String message) {
			log( message, Project.MSG_INFO );
		}

		public void warn(String message) {
			log( message, Project.MSG_WARN );
		}

		public void error(String message) {
			log( message, Project.MSG_ERR );
		}
	}

}
