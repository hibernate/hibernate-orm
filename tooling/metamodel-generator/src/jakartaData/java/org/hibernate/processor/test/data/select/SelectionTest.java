/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.select;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static java.nio.charset.Charset.defaultCharset;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class SelectionTest {

	@Test
	@WithClasses({
			SelectionStatus.class,
			SelectionPublisher.class,
			SelectionBook.class,
			SelectionRepository.class
	})
	void generatedRepositorySupportsSelectAndFirst() {
		final String repository = getMetaModelSourceAsString( SelectionRepository.class, true );
		System.out.println( repository );
		assertMetamodelClassGeneratedFor( SelectionBook.class );
		assertMetamodelClassGeneratedFor( SelectionRepository.class, true );

		assertTrue( repository.contains( "_query.select(_entity.get(SelectionBook_.title));" ) );
		assertTrue( repository.contains( "_query.select(_entity.get(SelectionBook_.pages));" ) );
		assertTrue( repository.contains(
				"_builder.construct(TitleAndPages.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" )
				|| repository.contains(
						"_builder.construct(SelectionRepository.TitleAndPages.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" ) );
		assertTrue( repository.contains(
				"_builder.construct(Renamed.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" )
				|| repository.contains(
						"_builder.construct(SelectionRepository.Renamed.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" ) );
		assertTrue( repository.contains(
				"_builder.construct(Named.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" )
				|| repository.contains(
						"_builder.construct(SelectionRepository.Named.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" ) );
		assertTrue( repository.contains( ".setMaxResults(1)" ) );
		assertTrue( repository.contains( ".setMaxResults(3)" ) );
		assertTrue( repository.contains( ".setMaxResults(2)" ) );
		assertTrue( repository.contains(
				"SelectionBook firstByStatus(SelectionStatus status, @Nonnull Order<SelectionBook> order)" ) );
		assertTrue( repository.contains( "for (var _sort : order.sorts())" ) );
		assertTrue( repository.contains( "select title from SelectionBook where status = :status" ) );
		assertTrue( repository.contains( "select title, pages from SelectionBook where status = :status" ) );
	}

	@Test
	void invalidSelectAndFirstUsesMeaningfulDiagnostics() throws Exception {
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final var compiler = ToolProvider.getSystemJavaCompiler();
		try ( var fileManager = compiler.getStandardFileManager( diagnostics, Locale.ROOT, defaultCharset() ) ) {
			final var sourceFiles = List.of(
					sourceFile( SelectionStatus.class ),
					sourceFile( SelectionPublisher.class ),
					sourceFile( SelectionBook.class ),
					sourceFile( InvalidSelectionRepository.class )
			);
			final var task = compiler.getTask(
					null,
					fileManager,
					diagnostics,
					List.of(
							"-d",
							TestUtil.getOutBaseDir( SelectionTest.class ).getAbsolutePath(),
							"-processor",
							HibernateProcessor.class.getName()
					),
					null,
					fileManager.getJavaFileObjectsFromFiles( sourceFiles )
			);
			assertFalse( task.call() );
		}
		final String messages = diagnostics.getDiagnostics()
				.stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.collect( Collectors.joining( "\n" ) );

		assertTrue( messages.contains( "no matching field named 'missing' in entity class" ) );
		assertTrue( messages.contains( "selected attribute 'publisher' of entity" ) );
		assertTrue( messages.contains( "is not a single-valued basic attribute" ) );
		assertTrue( messages.contains( "has type 'java.lang.String', which does not match method return type 'java.lang.Integer'" ) );
		assertTrue( messages.contains( "multiple '@Select' annotations require a record return type" ) );
		assertTrue( messages.contains( "'@First' value must be greater than 0" ) );
		assertTrue( messages.contains( "'@First' may not be combined with a parameter of type 'jakarta.data.Limit'" ) );
		assertTrue( messages.contains(
				"Jakarta Data repository method annotations are mutually exclusive: @Find, @Query" ) );
	}

	private static File sourceFile(Class<?> type) {
		return new File(
				TestUtil.getSourceBaseDir( type ),
				type.getName().replace( '.', File.separatorChar ) + ".java"
		);
	}
}
