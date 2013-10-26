package org.hibernate.bytecode.instrument.plugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryScanner;
import org.hibernate.bytecode.buildtime.internal.JavassistInstrumenter;
import org.hibernate.bytecode.buildtime.spi.Instrumenter.Options;

/**
 * Instruments class files to enable compile-time Hibernate field-interceptors.
 */
@Mojo(name = "instrument", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public final class InstrumentMojo extends AbstractMojo {
	/**
	 * The build directory, to perform instrumentation on. Defaults to ${project.build.directory}.
	 */
	@Parameter(property = "project.build.directory", readonly = true, required = true)
	private File buildDirectory = new File("");
	/**
	 * A list of file patterns to exclude from instrumentation. Defaults to nothing.
	 */
	@Parameter
	private String[] excludes = new String[] {};
	/**
	 * Whether or not extended instrumentation should be performed on class files. Defaults to false.
	 */
	@Parameter
	private boolean extendedInstrumentation = false;
	/**
	 * A list of file patterns to include in instrumentation. Defaults to all class files in the build directory.
	 */
	@Parameter
	private String[] includes = new String[] {"**/*.class"};
	
	@Component
	private Logger logger;
	
	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		JavassistInstrumenter task = new JavassistInstrumenter(new LoggerBridge(logger), new Options() {
			@Override
			public boolean performExtendedInstrumentation() {
				return extendedInstrumentation;
			}});
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(buildDirectory);
		scanner.setCaseSensitive(true);
		scanner.setIncludes(includes);
		scanner.setExcludes(excludes);
		scanner.scan();
		Set<File> files = new HashSet<File>();
		for (String file : scanner.getIncludedFiles()) {
			files.add(new File(buildDirectory, file));
		}
		task.execute(files);
	}
}
