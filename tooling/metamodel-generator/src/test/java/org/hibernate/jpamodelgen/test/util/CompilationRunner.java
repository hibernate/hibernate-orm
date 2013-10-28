/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
				mappingFiles.add( packageNameAsPath + File.separator + mappingFile );
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


