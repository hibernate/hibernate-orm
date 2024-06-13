/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.hibernate.processor.HibernateProcessor;

import org.junit.runners.model.Statement;

import org.jboss.logging.Logger;

import static org.junit.Assert.fail;

/**
 * A custom JUnit statement which will run annotation processor prior to execute the original statement/test.
 *
 * The classes to process are specified via {@code WithClasses}, {@code WithMappingFiles} and {@code WithProcessorOption}
 * on the actual test.
 *
 * @author Hardy Ferentschik
 */
public class CompilationStatement extends Statement {
	private static final Logger log = Logger.getLogger( CompilationStatement.class );
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String ANNOTATION_PROCESSOR_OPTION_PREFIX = "-A";

	private final Statement originalStatement;
	private final Class<?> testClass;
	private final List<Class<?>> testEntities;
	private final List<Class<?>> preCompileEntities;
	private final List<String> sources;
	private final List<String> xmlMappingFiles;
	private final Map<String, String> processorOptions;
	private final boolean ignoreCompilationErrors;
	private final List<Diagnostic<?>> compilationDiagnostics;

	public CompilationStatement(Statement originalStatement,
			Class<?> testClass,
			List<Class<?>> testEntities,
			List<Class<?>> proCompileEntities,
			List<String> sources,
			List<String> xmlMappingFiles,
			Map<String, String> processorOptions,
			boolean ignoreCompilationErrors) {
		this.originalStatement = originalStatement;
		this.testClass = testClass;
		this.testEntities = testEntities;
		this.preCompileEntities = proCompileEntities;
		this.sources = sources;
		this.xmlMappingFiles = xmlMappingFiles;
		this.processorOptions = processorOptions;
		this.ignoreCompilationErrors = ignoreCompilationErrors;
		this.compilationDiagnostics = new ArrayList<Diagnostic<?>>();
	}

	@Override
	public void evaluate() throws Throwable {
		// some test needs to compile some classes prior to the actual classes under test
		if ( !preCompileEntities.isEmpty() ) {
			compile( getCompilationUnits( preCompileEntities, null ) );
		}

		// now we compile the actual test classes
		compile( getCompilationUnits( testEntities, sources ) );

		if ( !ignoreCompilationErrors ) {
			TestUtil.assertNoCompilationError( compilationDiagnostics );
		}

		originalStatement.evaluate();
	}

	private List<File> getCompilationUnits(List<Class<?>> classesToCompile, List<String> sources) {
		List<File> javaFiles = new ArrayList<File>();
		for ( Class<?> testClass : classesToCompile ) {
			String pathToSource = getPathToSource( testClass );
			javaFiles.add( new File( pathToSource ) );
		}
		if ( sources != null ) {
			final var resourcesBaseDir = TestUtil.getResourcesBaseDir( testClass );
			for ( String source : sources ) {
				javaFiles.add(
						new File( resourcesBaseDir,
										 source.replace( PACKAGE_SEPARATOR, File.separator ) + ".java" ) );
			}

		}
		return javaFiles;
	}

	private String getPathToSource(Class<?> testClass) {
		return TestUtil.getSourceBaseDir( testClass ).getAbsolutePath() + File.separator + testClass.getName()
				.replace( PACKAGE_SEPARATOR, File.separator ) + ".java";
	}

	private void compile(List<File> sourceFiles) throws Exception {
		List<String> options = createJavaOptions();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager( diagnostics, null, null );
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
				sourceFiles
		);

		compileSources( options, compiler, diagnostics, fileManager, compilationUnits );
		compilationDiagnostics.addAll( diagnostics.getDiagnostics() );
		fileManager.close();
	}

	private List<String> createJavaOptions() {
		List<String> options = new ArrayList<String>();
		options.add( "-d" );
		options.add( TestUtil.getOutBaseDir( testClass ).getAbsolutePath() );
		options.add( "-processor" );
		options.add( HibernateProcessor.class.getName() );

		// pass orm files if specified
		if ( !xmlMappingFiles.isEmpty() ) {
			StringBuilder builder = new StringBuilder();
			builder.append( ANNOTATION_PROCESSOR_OPTION_PREFIX );
			builder.append( HibernateProcessor.ORM_XML_OPTION );
			builder.append( "=" );
			for ( String ormFile : xmlMappingFiles ) {
				builder.append( ormFile );
				builder.append( "," );
			}
			builder.deleteCharAt( builder.length() - 1 );
			options.add( builder.toString() );
		}

		// add any additional options specified by the test
		for ( Map.Entry<String, String> entry : processorOptions.entrySet() ) {
			StringBuilder builder = new StringBuilder();
			builder.append( ANNOTATION_PROCESSOR_OPTION_PREFIX );
			builder.append( entry.getKey() );
			builder.append( "=" );
			builder.append( entry.getValue() );
			options.add( builder.toString() );
		}
		return options;
	}

	private void compileSources(List<String> options,
			JavaCompiler compiler,
			DiagnosticCollector<JavaFileObject> diagnostics,
			StandardJavaFileManager fileManager,
			Iterable<? extends JavaFileObject> compilationUnits) {
		JavaCompiler.CompilationTask task = compiler.getTask(
				null, fileManager, diagnostics, options, null, compilationUnits
		);
		task.call();
		for ( Diagnostic<?> diagnostic : diagnostics.getDiagnostics() ) {
			log.debug( diagnostic.getMessage( null ) );
		}
	}
}


