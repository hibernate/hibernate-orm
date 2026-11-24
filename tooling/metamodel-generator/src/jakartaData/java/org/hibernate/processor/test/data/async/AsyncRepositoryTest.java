package org.hibernate.processor.test.data.async;



import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;
import org.hibernate.processor.test.util.CompilationDiagnostics;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
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
	@WithClasses({ AsyncBook.class, InvalidAsyncBookRepository.class })
	@IgnoreCompilationErrors
	void asynchronousMethodReturningNonCompletionStageUsesMeaningfulDiagnostic(CompilationDiagnostics diagnostics) {
		diagnostics.assertError( "method annotated '@Asynchronous' must return 'CompletionStage'" );
	}
}
