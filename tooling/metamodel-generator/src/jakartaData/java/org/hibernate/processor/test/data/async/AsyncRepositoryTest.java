/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.async;

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
class AsyncRepositoryTest {
	@Test
	@WithClasses({ AsyncBook.class, AsyncBookRepository.class })
	void generatedRepositorySupportsAsynchronousCompletionStageMethods() {
		final String repository = getMetaModelSourceAsString( AsyncBookRepository.class, true );

		assertTrue( repository.contains( "import static java.util.concurrent.CompletableFuture.completedStage;" ) );
		assertTrue( repository.contains( "import jakarta.enterprise.concurrent.Asynchronous;" ) );
		assertTrue( repository.contains( "@Asynchronous" ) );
		assertTrue( repository.contains( "CompletionStage<AsyncBook> bookByIsbn(" ) );
		assertTrue( repository.contains( "return completedStage(entityAgent.get(AsyncBook.class, isbn));" ) );
		assertTrue( repository.contains( "catch (EntityNotFoundException _ex) {\n"
				+ "\t\t\tthrow new EmptyResultException(_ex.getMessage(), _ex);" ) );
		assertTrue( repository.contains( "CompletionStage<List<AsyncBook>> booksByTitle(" ) );
		assertTrue( repository.contains( "return completedStage(_select" ) );
		assertTrue( repository.contains( "CompletionStage<Integer> updateTitle(" ) );
		assertTrue( repository.contains( "return completedStage(_select" ) );
		assertTrue( repository.contains( ".execute())" ) );
		assertTrue( repository.contains( "CompletionStage<Long> deleteByTitle(" ) );
		assertTrue( repository.contains( "return completedStage((long) entityAgent.createStatement(_query).execute())" ) );
		assertTrue( repository.contains( "CompletionStage<Void> insertBook(" ) );
		assertTrue( repository.contains( "@Nonnull\n\tpublic CompletionStage<Void> insertBook(" ) );
		assertTrue( repository.contains( "return completedStage(null);" ) );

		assertMetamodelClassGeneratedFor( AsyncBook.class, true );
		assertMetamodelClassGeneratedFor( AsyncBookRepository.class, true );
	}

	@Test
	void asynchronousMethodReturningNonCompletionStageUsesMeaningfulDiagnostic() throws Exception {
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final var compiler = ToolProvider.getSystemJavaCompiler();
		try ( var fileManager = compiler.getStandardFileManager( diagnostics, Locale.ROOT, defaultCharset() ) ) {
			final var sourceFiles = List.of(
					sourceFile( AsyncBook.class ),
					sourceFile( InvalidAsyncBookRepository.class )
			);
			final var task = compiler.getTask(
					null,
					fileManager,
					diagnostics,
					List.of(
							"-d",
							TestUtil.getOutBaseDir( AsyncRepositoryTest.class ).getAbsolutePath(),
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

		assertTrue( messages.contains( "method annotated '@Asynchronous' must return 'CompletionStage'" ) );
	}

	private static File sourceFile(Class<?> type) {
		return new File(
				TestUtil.getSourceBaseDir( type ),
				type.getName().replace( '.', File.separatorChar ) + ".java"
		);
	}
}
