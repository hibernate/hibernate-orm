/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.cursoredpage;

import java.util.NoSuchElementException;

import jakarta.data.page.PageRequest;

import org.hibernate.StatelessSession;
import org.hibernate.cfg.Configuration;
import org.hibernate.processor.test.data.namedquery.Author;
import org.hibernate.processor.test.data.namedquery.Book;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class CursoredBookRepositoryTest {
	@Test
	@WithClasses({ Book.class, Author.class, CursoredBookRepository.class })
	void test() {
		assertMetamodelClassGeneratedFor( CursoredBookRepository.class );
		final String source = getMetaModelSourceAsString( CursoredBookRepository.class );
		System.out.println( source );
		assertTrue( source.contains( "import java.util.List" ) );
	}

	@Test
	@WithClasses({ Book.class, Author.class, CursoredBookRepository.class })
	void testEmptyCursoredPageAfterDeletion() throws Exception {
		// HHH-21081: the extra row observed by the first query may disappear before the next request.
		try ( var factory = new Configuration()
				.addAnnotatedClass( Book.class )
				.addAnnotatedClass( Author.class )
				.setProperty( "hibernate.connection.url", "jdbc:h2:mem:hhh21081;DB_CLOSE_DELAY=-1" )
				.setProperty( "hibernate.hbm2ddl.auto", "create-drop" )
				.buildSessionFactory();
				var session = factory.openStatelessSession() ) {
			var transaction = session.beginTransaction();
			try {
				var repo = (CursoredBookRepository) getMetamodelClassFor( CursoredBookRepository.class )
						.getDeclaredConstructor( StatelessSession.class )
						.newInstance( session );
				var first = new Book( "isbn-1", "First", "First book" );
				var second = new Book( "isbn-2", "Second", "Second book" );
				session.insert( first );
				session.insert( second );

				var firstPage = repo.allBooks( PageRequest.ofSize( 1 ).withoutTotal() );
				assertEquals( 1, firstPage.content().size() );
				assertEquals( "isbn-1", session.getIdentifier( firstPage.content().get( 0 ) ) );
				assertTrue( firstPage.hasNext() );
				var nextRequest = firstPage.nextPageRequest();
				assertEquals( 2L, nextRequest.page() );

				session.delete( second );

				var emptyPage = repo.allBooks( nextRequest );
				assertEquals( nextRequest, emptyPage.pageRequest() );
				assertTrue( emptyPage.content().isEmpty() );
				assertFalse( emptyPage.hasNext() );
				assertFalse( emptyPage.hasPrevious() );
				assertThrows( NoSuchElementException.class, emptyPage::nextPageRequest );
				assertThrows( NoSuchElementException.class, emptyPage::previousPageRequest );
			}
			finally {
				transaction.rollback();
			}
		}
	}
}
