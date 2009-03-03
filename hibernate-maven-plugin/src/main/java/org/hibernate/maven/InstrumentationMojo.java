/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.maven;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.DirectoryScanner;

import org.hibernate.bytecode.buildtime.Instrumenter;
import org.hibernate.bytecode.buildtime.Logger;
import org.hibernate.bytecode.buildtime.JavassistInstrumenter;
import org.hibernate.bytecode.buildtime.CGLIBInstrumenter;

/**
 * @goal instrument
 * @phase process-classes
 * @requiresDependencyResolution
 *
 * @author Steve Ebersole
 */
public class InstrumentationMojo extends AbstractMojo implements Instrumenter.Options {
	/**
     * INTERNAL : The Maven Project to which we are attached
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

	/**
	 * Specifies the directory containing the classes to be instrumented.  By default we use the
	 * project's output directory, which in turn defaults to <samp>${basedir}/target/classes</samp>.
	 *
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 */
	private File instrumentationDirectory;

	/**
	 * @parameter
	 */
	private boolean extended;

	/**
	 * @parameter
	 */
	private String provider;

	public boolean performExtendedInstrumentation() {
		return extended;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		// first, lets determine whether to apply cglib or javassist based instrumentation...
		if ( provider == null ) {
			provider = determineProvider();
			if ( provider == null ) {
				throw new MojoExecutionException( "Unable to determine provider to use" );
			}
		}

		Instrumenter instrumenter = resolveInstrumenter( provider, new LoggingBridge() );
		try {
			instrumenter.execute( collectFilesToProcess() );
		}
		catch (  Throwable t ) {
			throw new MojoExecutionException( "Error executing instrumentation", t );
		}
	}

	private Set collectFilesToProcess() {
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir( instrumentationDirectory );
		scanner.setIncludes( new String[] { "**/*.class" } );
		scanner.addDefaultExcludes();
		scanner.scan();
		String[] includedFiles = scanner.getIncludedFiles();
		HashSet fileSet = new HashSet( includedFiles.length + (int)(.75*includedFiles.length) + 1 );
		fileSet.addAll( Arrays.asList( includedFiles ) );
		return fileSet;
	}

	private Instrumenter resolveInstrumenter(String provider, Logger logger) throws MojoExecutionException {
		if ( "javassist".equals( provider ) ) {
			return new JavassistInstrumenter( logger, this );
		}
		else if ( "cglib".equals( provider ) ) {
			return new CGLIBInstrumenter( logger, this );
		}
		else {
			throw new MojoExecutionException( "Unable to resolve provider [" + provider + "] to appropriate instrumenter" );
		}
	}

	/**
	 * Determine the provider to use.  Called in the cases where the user did not explicitly specify; so we look
	 * through the dependencies for the project and decide which provider should be applied.
	 * <p/>
	 * NOTE: this impl prefers javassist.
	 *
	 * @return The provider determined from project's dependencies.
	 */
	private String determineProvider() {
		if ( project.getCompileArtifacts() != null ) {
			boolean foundCglib = false;
			Iterator itr = project.getCompileArtifacts().iterator();
			while ( itr.hasNext() ) {
				final Artifact artifact = ( Artifact ) itr.next();
				if ( "javassist".equals( artifact.getGroupId() ) && "javassist".equals( artifact.getArtifactId() ) ) {
					return "javassist";
				}
				else if ( "org.hibernate".equals( artifact.getGroupId() )
						&& "hibernate-cglib-repack".equals( artifact.getArtifactId() ) ) {
					foundCglib = true;
				}
			}
			if ( foundCglib ) {
				return "cglib";
			}
		}
		return null;
	}

	private class LoggingBridge implements Logger {
		public void trace(String message) {
			getLog().debug( message );
		}

		public void debug(String message) {
			getLog().debug( message );
		}

		public void info(String message) {
			getLog().info( message );
		}

		public void warn(String message) {
			getLog().warn( message );
		}

		public void error(String message) {
			getLog().error( message );
		}
	}
}
