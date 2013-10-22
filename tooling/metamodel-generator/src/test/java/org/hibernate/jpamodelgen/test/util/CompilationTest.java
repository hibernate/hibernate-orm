/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.jpamodelgen.test.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;

import static org.testng.FileAssert.fail;

/**
 * Base class for annotation processor tests.
 *
 * @author Hardy Ferentschik
 */
public abstract class CompilationTest {
	private static final Logger log = LoggerFactory.getLogger( CompilationTest.class );
	private static final String ANNOTATION_PROCESSOR_OPTION_PREFIX = "-A";
	private static final String SOURCE_BASE_DIR_PROPERTY = "sourceBaseDir";
	private static final String OUT_BASE_DIR_PROPERTY = "outBaseDir";
	private static final String SOURCE_BASE_DIR;
	private static final String OUT_BASE_DIR;

	public static final String DIRECTORY_SEPARATOR = File.separator;

	private List<Diagnostic<?>> compilationDiagnostics;

	static {
		String tmp = System.getProperty( SOURCE_BASE_DIR_PROPERTY );
		if ( tmp == null ) {
			fail( "The system property sourceBaseDir has to be set and point to the base directory of the test java sources." );
		}
		SOURCE_BASE_DIR = tmp;

		tmp = System.getProperty( OUT_BASE_DIR_PROPERTY );
		if ( tmp == null ) {
			fail( "The system property outBaseDir has to be set and point to the base directory of the test output directory." );
		}
		OUT_BASE_DIR = tmp;
	}

	public CompilationTest() {
		compilationDiagnostics = new ArrayList<Diagnostic<?>>();
	}

	public final List<Diagnostic<?>> getCompilationDiagnostics() {
		return compilationDiagnostics;
	}

	public static String getSourceBaseDir() {
		return SOURCE_BASE_DIR;
	}

	@BeforeClass
	protected void compileAllTestEntities() throws Exception {
		List<File> sourceFiles = getCompilationUnits( SOURCE_BASE_DIR, getPackageNameOfCurrentTest() );
		// make sure there are no relics from previous runs
		TestUtil.deleteGeneratedSourceFiles( new File( OUT_BASE_DIR ) );
		compile( sourceFiles );
	}

	/**
	 * Compiles the specified Java classes and generated the meta model java files which in turn get also compiled.
	 *
	 * @param sourceFiles the files containing the java source files to compile.
	 *
	 * @throws Exception in case the compilation fails
	 */
	protected void compile(List<File> sourceFiles) throws Exception {
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

	protected List<File> getCompilationUnits(String baseDir, String packageName) {
		List<File> javaFiles = new ArrayList<File>();
		String packageDirName = baseDir;
		if ( packageName != null ) {
			packageDirName = packageDirName + DIRECTORY_SEPARATOR + packageName.replace( ".", DIRECTORY_SEPARATOR );
		}

		File packageDir = new File( packageDirName );
		FilenameFilter javaFileFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith( ".java" ) && !name.endsWith( "Test.java" );
			}
		};
		final File[] files = packageDir.listFiles( javaFileFilter );
		if ( files == null ) {
			throw new RuntimeException( "Cannot find package directory (is your base dir correct?): " + packageDirName );
		}
		javaFiles.addAll( Arrays.asList( files ) );
		return javaFiles;
	}

	protected abstract String getPackageNameOfCurrentTest();

	protected Map<String, String> getProcessorOptions() {
		return Collections.emptyMap();
	}

	protected Collection<String> getOrmFiles() {
		return Collections.emptyList();
	}

	private void compileSources(List<String> options, JavaCompiler compiler, DiagnosticCollector<JavaFileObject> diagnostics, StandardJavaFileManager fileManager, Iterable<? extends JavaFileObject> compilationUnits) {
		JavaCompiler.CompilationTask task = compiler.getTask(
				null, fileManager, diagnostics, options, null, compilationUnits
		);
		task.call();
		for ( Diagnostic<?> diagnostic : diagnostics.getDiagnostics() ) {
			log.debug( diagnostic.getMessage( null ) );
		}
	}

	private List<String> createJavaOptions() {
		List<String> options = new ArrayList<String>();
		options.add( "-d" );
		options.add( OUT_BASE_DIR );

		// pass orm files if specified
		if ( !getOrmFiles().isEmpty() ) {
			StringBuilder builder = new StringBuilder();
			builder.append( ANNOTATION_PROCESSOR_OPTION_PREFIX );
			builder.append( JPAMetaModelEntityProcessor.ORM_XML_OPTION );
			builder.append( "=" );
			for ( String ormFile : getOrmFiles() ) {
				builder.append( ormFile );
				builder.append( "," );
			}
			builder.deleteCharAt( builder.length() - 1 );
			options.add( builder.toString() );
		}

		// add any additional options specified by the test
		for ( Map.Entry<String, String> entry : getProcessorOptions().entrySet() ) {
			StringBuilder builder = new StringBuilder();
			builder.append( ANNOTATION_PROCESSOR_OPTION_PREFIX );
			builder.append( entry.getKey() );
			builder.append( "=" );
			builder.append( entry.getValue() );
			options.add( builder.toString() );
		}
		return options;
	}
}


