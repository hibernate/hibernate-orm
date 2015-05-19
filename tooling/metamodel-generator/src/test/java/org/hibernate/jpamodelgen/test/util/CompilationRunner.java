/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Custom JUnit runner which makes sure the annotation processor runs prior to the test method.
 *
 * @author Hardy Ferentschik
 * @see CompilationStatement
 */
public class CompilationRunner extends BlockJUnit4ClassRunner {
	private final List<Class<?>> testEntities;
	private final List<Class<?>> preCompileEntities;
	private final List<String> mappingFiles;
	private final Map<String, String> processorOptions;
	private final String packageName;
	private boolean ignoreCompilationErrors;


	public CompilationRunner(Class<?> clazz) throws InitializationError {
		super( clazz );
		this.testEntities = new ArrayList<Class<?>>();
		this.preCompileEntities = new ArrayList<Class<?>>();
		this.mappingFiles = new ArrayList<String>();
		this.processorOptions = new HashMap<String, String>();
		Package pkg = clazz.getPackage();
		this.packageName = pkg != null ? pkg.getName() : null;

		processWithClasses( clazz.getAnnotation( WithClasses.class ) );
		processWithMappingFiles( clazz.getAnnotation( WithMappingFiles.class ) );
		processOptions(
				clazz.getAnnotation( WithProcessorOption.class ),
				clazz.getAnnotation( WithProcessorOption.List.class )
		);

		ignoreCompilationErrors = clazz.getAnnotation( IgnoreCompilationErrors.class ) != null;
	}

	@Override
	protected Statement methodBlock(FrameworkMethod method) {
		Statement statement = super.methodBlock( method );
		processAnnotations( method );
		if ( !annotationProcessorNeedsToRun() ) {
			return statement;
		}

		return new CompilationStatement(
				statement,
				testEntities,
				preCompileEntities,
				mappingFiles,
				processorOptions,
				ignoreCompilationErrors
		);
	}

	private void processWithClasses(WithClasses withClasses) {
		if ( withClasses != null ) {
			Collections.addAll( testEntities, withClasses.value() );
			Collections.addAll( preCompileEntities, withClasses.preCompile() );
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

	private void processAnnotations(FrameworkMethod method) {
		// configuration will be added to potential class level configuration
		processWithClasses( method.getAnnotation( WithClasses.class ) );
		processWithMappingFiles( method.getAnnotation( WithMappingFiles.class ) );
		processOptions(
				method.getAnnotation( WithProcessorOption.class ),
				method.getAnnotation( WithProcessorOption.List.class )
		);

		// overrides potential class level configuration
		ignoreCompilationErrors = method.getAnnotation( IgnoreCompilationErrors.class ) != null;
	}

	private void addOptions(WithProcessorOption withProcessorOptionsAnnotation) {
		if ( withProcessorOptionsAnnotation != null ) {
			processorOptions.put( withProcessorOptionsAnnotation.key(), withProcessorOptionsAnnotation.value() );
		}
	}

	private boolean annotationProcessorNeedsToRun() {
		return !testEntities.isEmpty() || !mappingFiles.isEmpty();
	}
}


