/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.util;

import org.hibernate.processor.HibernateProcessor;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompilationExtension
		implements Extension, BeforeEachCallback, AfterEachCallback, TestInstancePreConstructCallback {

	private static final Logger log = Logger.getLogger( CompilationExtension.class );
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String ANNOTATION_PROCESSOR_OPTION_PREFIX = "-A";
	private static final String COMPILATION_TEST_INFO = "CompilationTestInfo";

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		CompilationTestInfo compilationTestInfo = getCompilationTestInfo( context )
				.forMethod( context.getRequiredTestMethod() );
		if ( compilationTestInfo.annotationProcessorNeedsToRun() ) {
			final List<Diagnostic<?>> compilationDiagnostics =  new ArrayList<>();
			// some test needs to compile some classes prior to the actual classes under test
			if ( !compilationTestInfo.preCompileEntities.isEmpty() ) {
				compile( getCompilationUnits( compilationTestInfo.preCompileEntities, null, compilationTestInfo ), compilationDiagnostics, compilationTestInfo );
			}

			// now we compile the actual test classes
			compile( getCompilationUnits( compilationTestInfo.testEntities, compilationTestInfo.sources,
					compilationTestInfo ), compilationDiagnostics, compilationTestInfo );

			if ( !compilationTestInfo.ignoreCompilationErrors ) {
				TestUtil.assertNoCompilationError( compilationDiagnostics );
			}
		}
	}

	@Override
	public void afterEach(ExtensionContext context) {
		context.getTestClass().ifPresent( TestUtil::deleteProcessorGeneratedFiles );
	}

	@Override
	public void preConstructTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext context) {
		Class<?> testClass = factoryContext.getTestClass();

		var store = context.getStore( ExtensionContext.StoreScope.EXTENSION_CONTEXT,
				ExtensionContext.Namespace.create( testClass ) );

		store.put( COMPILATION_TEST_INFO, new CompilationTestInfo( testClass ) );
	}

	private CompilationTestInfo getCompilationTestInfo(ExtensionContext context) {
		var store = context.getStore( ExtensionContext.StoreScope.EXTENSION_CONTEXT,
				ExtensionContext.Namespace.create( context.getRequiredTestClass() ) );

		return store.get( COMPILATION_TEST_INFO, CompilationTestInfo.class );
	}

	private List<File> getCompilationUnits(List<Class<?>> classesToCompile, List<String> sources, CompilationTestInfo compilationTestInfo) {
		List<File> javaFiles = new ArrayList<File>();
		for ( Class<?> testClass : classesToCompile ) {
			String pathToSource = getPathToSource( testClass );
			javaFiles.add( new File( pathToSource ) );
		}
		if ( sources != null ) {
			final var resourcesBaseDir = TestUtil.getResourcesBaseDir( compilationTestInfo.testClass );
			for ( String source : sources ) {
				javaFiles.add(
						new File( resourcesBaseDir,
								source.replace( PACKAGE_SEPARATOR, File.separator ) + ".java" ) );
			}

		}
		return javaFiles;
	}

	private String getPathToSource(Class<?> testClass) {
		if ( testClass.isMemberClass() ) {
			return getPathToSource( testClass.getDeclaringClass() );
		}
		return TestUtil.getSourceBaseDir( testClass ).getAbsolutePath() + File.separator + testClass.getName()
				.replace( PACKAGE_SEPARATOR, File.separator ) + ".java";
	}

	private void compile(List<File> sourceFiles, List<Diagnostic<?>> compilationDiagnostics, CompilationTestInfo compilationTestInfo) throws Exception {
		List<String> options = createJavaOptions(compilationTestInfo);

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

	private List<String> createJavaOptions(CompilationTestInfo compilationTestInfo) {
		List<String> options = new ArrayList<>();
		options.add( "-d" );
		options.add( TestUtil.getOutBaseDir( compilationTestInfo.testClass ).getAbsolutePath() );
		options.add( "-processor" );
		options.add( HibernateProcessor.class.getName() );

		// pass orm files if specified
		if ( !compilationTestInfo.mappingFiles.isEmpty() ) {
			StringBuilder builder = new StringBuilder();
			builder.append( ANNOTATION_PROCESSOR_OPTION_PREFIX );
			builder.append( HibernateProcessor.ORM_XML_OPTION );
			builder.append( "=" );
			for ( String ormFile : compilationTestInfo.mappingFiles ) {
				builder.append( ormFile );
				builder.append( "," );
			}
			builder.deleteCharAt( builder.length() - 1 );
			options.add( builder.toString() );
		}

		// add any additional options specified by the test
		for ( Map.Entry<String, String> entry : compilationTestInfo.processorOptions.entrySet() ) {
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

	private static class CompilationTestInfo {
		private final Class<?> testClass;
		private final List<Class<?>> testEntities;
		private final List<Class<?>> preCompileEntities;
		private final List<String> sources;
		private final List<String> mappingFiles;
		private final Map<String, String> processorOptions;
		private final String packageName;
		private final boolean ignoreCompilationErrors;

		private CompilationTestInfo(Class<?> testClass, List<Class<?>> testEntities, List<Class<?>> preCompileEntities, List<String> sources, List<String> mappingFiles, Map<String, String> processorOptions, String packageName, boolean ignoreCompilationErrors) {
			this.testClass = testClass;
			this.testEntities = testEntities;
			this.preCompileEntities = preCompileEntities;
			this.sources = sources;
			this.mappingFiles = mappingFiles;
			this.processorOptions = processorOptions;
			this.packageName = packageName;
			this.ignoreCompilationErrors = ignoreCompilationErrors;
		}

		private CompilationTestInfo(Class<?> testClass) {
			this.testClass = testClass;
			this.testEntities = new ArrayList<>();
			this.preCompileEntities = new ArrayList<>();
			this.sources = new ArrayList<>();
			this.mappingFiles = new ArrayList<>();
			this.processorOptions = new HashMap<>();
			Package pkg = testClass.getPackage();
			this.packageName = pkg != null ? pkg.getName() : null;

			processWithClasses( testClass.getAnnotation( WithClasses.class ) );
			processWithMappingFiles( testClass.getAnnotation( WithMappingFiles.class ) );
			processOptions(
					testClass.getAnnotation( WithProcessorOption.class ),
					testClass.getAnnotation( WithProcessorOption.List.class )
			);

			ignoreCompilationErrors = testClass.getAnnotation( IgnoreCompilationErrors.class ) != null;
		}

		protected CompilationTestInfo forMethod(Method method) {

			CompilationTestInfo copy = new CompilationTestInfo(
					testClass,
					new ArrayList<>( testEntities ),
					new ArrayList<>( preCompileEntities ),
					new ArrayList<>( sources ),
					new ArrayList<>( mappingFiles ),
					new HashMap<>( processorOptions ),
					packageName,
					// overrides potential class level configuration
					ignoreCompilationErrors || method.getAnnotation( IgnoreCompilationErrors.class ) != null
			);
			copy.processAnnotations( method );

			return copy;
		}

		private void processAnnotations(Method method) {
			// configuration will be added to potential class level configuration
			processWithClasses( method.getAnnotation( WithClasses.class ) );
			processWithMappingFiles( method.getAnnotation( WithMappingFiles.class ) );
			processOptions(
					method.getAnnotation( WithProcessorOption.class ),
					method.getAnnotation( WithProcessorOption.List.class )
			);
		}

		private void processWithClasses(WithClasses withClasses) {
			if ( withClasses != null ) {
				Collections.addAll( testEntities, withClasses.value() );
				Collections.addAll( preCompileEntities, withClasses.preCompile() );
				Collections.addAll( sources, withClasses.sources() );
			}
		}

		private void processWithMappingFiles(WithMappingFiles withMappingFiles) {
			if ( withMappingFiles != null ) {
				String packageNameAsPath = TestUtil.fcnToPath( packageName );
				for ( String mappingFile : withMappingFiles.value() ) {
					mappingFiles.add( packageNameAsPath + TestUtil.RESOURCE_SEPARATOR + mappingFile );
				}
			}
		}

		private void processOptions(WithProcessorOption withProcessorOption,
									WithProcessorOption.List withProcessorOptionsListAnnotation) {
			addOptions( withProcessorOption );
			if ( withProcessorOptionsListAnnotation != null ) {
				for ( WithProcessorOption option : withProcessorOptionsListAnnotation.value() ) {
					addOptions( option );
				}
			}
		}

		private void addOptions(WithProcessorOption withProcessorOptionsAnnotation) {
			if ( withProcessorOptionsAnnotation != null ) {
				processorOptions.put( withProcessorOptionsAnnotation.key(), withProcessorOptionsAnnotation.value() );
			}
		}

		private boolean annotationProcessorNeedsToRun() {
			return !testEntities.isEmpty() || !sources.isEmpty() || !mappingFiles.isEmpty();
		}
	}
}
