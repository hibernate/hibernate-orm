// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.testng.FileAssert.fail;

/**
 * @author Hardy Ferentschik
 */
public abstract class CompilationTest {

	private static final String PATH_SEPARATOR = System.getProperty( "file.separator" );
	private static final String sourceBaseDir;
	private static final String outBaseDir;

	static {
		String tmp = System.getProperty( "sourceBaseDir" );
		if ( tmp == null ) {
			fail( "The system property sourceBaseDir has to be set and point to the base directory of the test java sources." );
		}
		sourceBaseDir = tmp;

		tmp = System.getProperty( "outBaseDir" );
		if ( tmp == null ) {
			fail( "The system property outBaseDir has to be set and point to the base directory of the test output directory." );
		}
		outBaseDir = tmp;
	}

	public CompilationTest() {
		try {
			compile();
		}
		catch ( Exception e ) {
			fail( "Unable to compile test sources. " + e.getMessage() );
		}
	}

	private void compile() throws IOException {
		List<String> options = createJavaOptions();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager( diagnostics, null, null );
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
				getCompilationUnits( sourceBaseDir )
		);

		// TODO - need to call the compiler twice. Once to compile the test classes and generate the java files
		// of the generated metamodel. The second compile is for generated the class files of the metamodel.
		// Note sure why this is not recursive the same way as on the command line
		compileSources( options, compiler, diagnostics, fileManager, compilationUnits );

		compilationUnits = fileManager.getJavaFileObjectsFromFiles(
				getCompilationUnits( outBaseDir )
		);
		compileSources( options, compiler, diagnostics, fileManager, compilationUnits );
		fileManager.close();
	}

	private void compileSources(List<String> options, JavaCompiler compiler, DiagnosticCollector<JavaFileObject> diagnostics, StandardJavaFileManager fileManager, Iterable<? extends JavaFileObject> compilationUnits) {
		JavaCompiler.CompilationTask task = compiler.getTask(
				null, fileManager, diagnostics, options, null, compilationUnits
		);
		task.call();
//		for ( Diagnostic diagnostic : diagnostics.getDiagnostics() ) {
//			System.out.println( diagnostic.getMessage( null ) );
//		}
	}

	private List<String> createJavaOptions() {
		// TODO
		// passing any other options as -d seems to throw IllegalArgumentExceptions. I would like to set -s for example
		// in order to see whether recursive recompilation would work then. Also '-proc only' could be interesting
		List<String> options = new ArrayList<String>();
		options.add( "-d" );
		options.add( outBaseDir );
		return options;
	}

	private List<File> getCompilationUnits(String baseDir) {
		List<File> javaFiles = new ArrayList<File>();
		String packageDirName = baseDir + PATH_SEPARATOR + getTestPackage().replace( ".", PATH_SEPARATOR );
		File packageDir = new File( packageDirName );
		FilenameFilter javaFileFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith( ".java" ) && !name.endsWith( "Test.java" );
			}
		};
		for ( File file : packageDir.listFiles( javaFileFilter ) ) {
			javaFiles.add( file );
		}
		return javaFiles;
	}

	abstract protected String getTestPackage();
}


