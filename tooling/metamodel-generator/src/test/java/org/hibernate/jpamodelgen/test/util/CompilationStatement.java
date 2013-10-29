package org.hibernate.jpamodelgen.test.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger( CompilationStatement.class );
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String ANNOTATION_PROCESSOR_OPTION_PREFIX = "-A";
	private static final String SOURCE_BASE_DIR_PROPERTY = "sourceBaseDir";
	private static final String SOURCE_BASE_DIR;

	static {
		// first we try to guess the target directory.
		File potentialSourceDirectory = new File(System.getProperty( "user.dir" ), "tooling/metamodel-generator/src/test/java");

		// the command line build sets the user.dir to sub project directory
		if ( !potentialSourceDirectory.exists() ) {
			potentialSourceDirectory = new File(System.getProperty( "user.dir" ), "src/test/java");
		}

		if ( potentialSourceDirectory.exists() ) {
			SOURCE_BASE_DIR = potentialSourceDirectory.getAbsolutePath();
		}
		else {
			String tmp = System.getProperty( SOURCE_BASE_DIR_PROPERTY );
			if ( tmp == null ) {
				fail(
						"Unable to guess determine the source directory. Specify the system property 'sourceBaseDir'" +
								" pointing to the base directory of the test java sources."
				);
			}
			SOURCE_BASE_DIR = tmp;
		}
	}

	private final Statement originalStatement;
	private final List<Class<?>> testEntities;
	private final List<Class<?>> preCompileEntities;
	private final List<String> xmlMappingFiles;
	private final Map<String, String> processorOptions;
	private final boolean ignoreCompilationErrors;
	private final List<Diagnostic<?>> compilationDiagnostics;

	public CompilationStatement(Statement originalStatement,
			List<Class<?>> testEntities,
			List<Class<?>> proCompileEntities,
			List<String> xmlMappingFiles,
			Map<String, String> processorOptions,
			boolean ignoreCompilationErrors) {
		this.originalStatement = originalStatement;
		this.testEntities = testEntities;
		this.preCompileEntities = proCompileEntities;
		this.xmlMappingFiles = xmlMappingFiles;
		this.processorOptions = processorOptions;
		this.ignoreCompilationErrors = ignoreCompilationErrors;
		this.compilationDiagnostics = new ArrayList<Diagnostic<?>>();
	}

	@Override
	public void evaluate() throws Throwable {
		try {
			// some test needs to compile some classes prior to the actual classes under test
			if ( !preCompileEntities.isEmpty() ) {
				compile( getCompilationUnits( preCompileEntities ) );
			}

			// now we compile the actual test classes
			compile( getCompilationUnits( testEntities ) );

			if ( !ignoreCompilationErrors ) {
				TestUtil.assertNoCompilationError( compilationDiagnostics );
			}
		}
		catch ( Exception e ) {
			StringWriter errors = new StringWriter();
			e.printStackTrace( new PrintWriter( errors ) );
			log.debug( errors.toString() );
			fail( "Unable to process test sources." );
		}
		originalStatement.evaluate();
	}

	private List<File> getCompilationUnits(List<Class<?>> classesToCompile) {
		List<File> javaFiles = new ArrayList<File>();
		for ( Class<?> testClass : classesToCompile ) {
			String pathToSource = getPathToSource( testClass );
			javaFiles.add( new File( pathToSource ) );
		}
		return javaFiles;
	}

	private String getPathToSource(Class<?> testClass) {
		return SOURCE_BASE_DIR + File.separator + testClass.getName()
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
		options.add( TestUtil.getOutBaseDir().getAbsolutePath() );

		// pass orm files if specified
		if ( !xmlMappingFiles.isEmpty() ) {
			StringBuilder builder = new StringBuilder();
			builder.append( ANNOTATION_PROCESSOR_OPTION_PREFIX );
			builder.append( JPAMetaModelEntityProcessor.ORM_XML_OPTION );
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


